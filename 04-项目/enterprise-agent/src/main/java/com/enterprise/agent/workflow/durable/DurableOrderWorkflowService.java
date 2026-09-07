package com.enterprise.agent.workflow.durable;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Week 6.5 Day 2：支持 Checkpoint、恢复、幂等、人工审批和补偿的订单 Workflow。
 */
@Service
public class DurableOrderWorkflowService {

    private final DurableWorkflowPlannerAssistant planner;
    private final AgentCheckpointStore checkpointStore;
    private final IdempotentToolExecutor toolExecutor;
    private final WorkflowApprovalService approvalService;
    private final OrderWorkflowGateway orderGateway;
    private final NotificationGateway notificationGateway;
    private final AuditLogService auditLog;

    @Autowired
    public DurableOrderWorkflowService(@Qualifier("openAiChatModel") OpenAiChatModel chatModel,
                                       AgentCheckpointStore checkpointStore,
                                       IdempotentToolExecutor toolExecutor,
                                       WorkflowApprovalService approvalService,
                                       OrderWorkflowGateway orderGateway,
                                       NotificationGateway notificationGateway,
                                       AuditLogService auditLog) {
        this(
                AiServices.builder(DurableWorkflowPlannerAssistant.class)
                        .chatModel(chatModel)
                        .build(),
                checkpointStore,
                toolExecutor,
                approvalService,
                orderGateway,
                notificationGateway,
                auditLog
        );
    }

    DurableOrderWorkflowService(DurableWorkflowPlannerAssistant planner,
                                AgentCheckpointStore checkpointStore,
                                IdempotentToolExecutor toolExecutor,
                                WorkflowApprovalService approvalService,
                                OrderWorkflowGateway orderGateway,
                                NotificationGateway notificationGateway,
                                AuditLogService auditLog) {
        this.planner = planner;
        this.checkpointStore = checkpointStore;
        this.toolExecutor = toolExecutor;
        this.approvalService = approvalService;
        this.orderGateway = orderGateway;
        this.notificationGateway = notificationGateway;
        this.auditLog = auditLog;
    }

    /**
     * 创建任务后只生成计划和审批单，不执行任何业务副作用。
     */
    public AgentRun start(String userId,
                          String tenantId,
                          String idempotencyKey,
                          String orderId,
                          String newStatus) {
        requireText(userId, "userId");
        requireText(tenantId, "tenantId");
        requireText(idempotencyKey, "idempotencyKey");
        requireText(orderId, "orderId");
        requireText(newStatus, "newStatus");

        AgentRun existing = checkpointStore
                .findByIdempotencyKey(tenantId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            ensureSameRequest(existing, userId, orderId, newStatus);
            return existing;
        }

        DurableWorkflowPlan plan = DurableWorkflowPlan.fromModelOutput(
                planner.plan(orderId, newStatus));
        String runId = UUID.randomUUID().toString();
        String originalStatus = orderGateway.currentStatus(orderId);

        if (plan.requiresHuman()) {
            AgentRun rejected = new AgentRun(
                    runId, userId, tenantId, idempotencyKey, orderId, newStatus, originalStatus,
                    List.of(), List.of(), null, AgentRunStatus.FAILED,
                    null, plan.reason(), Instant.now());
            checkpointStore.save(rejected);
            audit(rejected, "plan", plan.reason(), AuditStatus.DENIED);
            return rejected;
        }

        if (!"PENDING".equals(originalStatus)) {
            AgentRun rejected = new AgentRun(
                    runId, userId, tenantId, idempotencyKey, orderId, newStatus, originalStatus,
                    plan.steps(), List.of(), null, AgentRunStatus.FAILED,
                    null, "只有 PENDING 订单可以进入修改流程", Instant.now());
            checkpointStore.save(rejected);
            audit(rejected, "validateOrder", rejected.lastError(), AuditStatus.DENIED);
            return rejected;
        }

        WorkflowApprovalService.WorkflowApproval approval = approvalService.request(
                tenantId,
                runId,
                "订单 " + orderId + "：" + originalStatus + " -> " + newStatus + "，随后通知用户"
        );
        AgentRun waiting = new AgentRun(
                runId, userId, tenantId, idempotencyKey, orderId, newStatus, originalStatus,
                plan.steps(), List.of(), plan.steps().getFirst(), AgentRunStatus.WAITING_APPROVAL,
                approval.approvalId(), null, Instant.now());
        checkpointStore.save(waiting);
        audit(waiting, "requestApproval", approval.approvalId(), AuditStatus.PENDING_APPROVAL);
        return waiting;
    }

    /**
     * 从 Checkpoint 指向的下一步继续；已完成的步骤不会重新进入业务动作。
     */
    public AgentRun resume(String tenantId, String runId) {
        AgentRun run = requireRun(tenantId, runId);
        if (isTerminal(run.status())) {
            return run;
        }
        if (run.status() == AgentRunStatus.COMPENSATION_FAILED || run.nextStep() == null) {
            return run;
        }
        if (!approvalService.isApproved(tenantId, runId)) {
            AgentRun waiting = run.transition(
                    AgentRunStatus.WAITING_APPROVAL,
                    run.nextStep(),
                    run.completedSteps(),
                    null
            );
            checkpointStore.save(waiting);
            return waiting;
        }

        run = run.transition(AgentRunStatus.RUNNING, run.nextStep(), run.completedSteps(), null);
        checkpointStore.save(run);

        while (run.nextStep() != null) {
            AgentStep currentStep = run.nextStep();
            try {
                ToolExecutionResult execution = executeStep(run, currentStep);
                List<AgentStep> completed = new ArrayList<>(run.completedSteps());
                if (!completed.contains(currentStep)) {
                    completed.add(currentStep);
                }
                AgentStep nextStep = nextStep(run.plannedSteps(), currentStep);
                AgentRunStatus newStatus = nextStep == null
                        ? AgentRunStatus.COMPLETED
                        : AgentRunStatus.RUNNING;
                run = run.transition(newStatus, nextStep, completed, null);
                checkpointStore.save(run);
                audit(run, currentStep.name(),
                        execution.result() + (execution.replayed() ? "（幂等结果重放）" : ""),
                        AuditStatus.SUCCESS);
            } catch (RuntimeException e) {
                run = run.transition(
                        AgentRunStatus.FAILED,
                        currentStep,
                        run.completedSteps(),
                        e.getMessage()
                );
                checkpointStore.save(run);
                audit(run, currentStep.name(), e.getMessage(), AuditStatus.FAILED);
                return run;
            }
        }
        return run;
    }

    /**
     * 通知失败后，可把已经修改的订单恢复到原状态。补偿失败会被保存，方便人工继续处理。
     */
    public AgentRun compensate(String tenantId, String runId) {
        AgentRun run = requireRun(tenantId, runId);
        if (run.status() != AgentRunStatus.FAILED
                && run.status() != AgentRunStatus.COMPENSATION_FAILED) {
            throw new IllegalStateException("只有失败的 Agent Run 可以补偿");
        }
        if (!run.completedSteps().contains(AgentStep.UPDATE_ORDER)) {
            return run;
        }

        try {
            ToolExecutionResult result = toolExecutor.execute(
                    tenantId,
                    runId + ":COMPENSATE_UPDATE_ORDER",
                    () -> orderGateway.updateStatus(run.orderId(), run.originalStatus())
            );
            AgentRun compensated = run.transition(
                    AgentRunStatus.COMPENSATED,
                    null,
                    run.completedSteps(),
                    null
            );
            checkpointStore.save(compensated);
            audit(compensated, "COMPENSATE_UPDATE_ORDER", result.result(), AuditStatus.COMPENSATED);
            return compensated;
        } catch (RuntimeException e) {
            AgentRun failed = run.transition(
                    AgentRunStatus.COMPENSATION_FAILED,
                    run.nextStep(),
                    run.completedSteps(),
                    e.getMessage()
            );
            checkpointStore.save(failed);
            audit(failed, "COMPENSATE_UPDATE_ORDER", e.getMessage(), AuditStatus.FAILED);
            return failed;
        }
    }

    private ToolExecutionResult executeStep(AgentRun run, AgentStep step) {
        return switch (step) {
            case UPDATE_ORDER -> toolExecutor.execute(
                    run.tenantId(),
                    run.runId() + ":UPDATE_ORDER",
                    () -> orderGateway.updateStatus(run.orderId(), run.newStatus())
            );
            case SEND_NOTIFICATION -> toolExecutor.execute(
                    run.tenantId(),
                    run.runId() + ":SEND_NOTIFICATION",
                    () -> notificationGateway.sendStatusChanged(
                            run.userId(), run.orderId(), run.newStatus())
            );
        };
    }

    private AgentStep nextStep(List<AgentStep> steps, AgentStep currentStep) {
        int currentIndex = steps.indexOf(currentStep);
        int nextIndex = currentIndex + 1;
        return nextIndex >= steps.size() ? null : steps.get(nextIndex);
    }

    private AgentRun requireRun(String tenantId, String runId) {
        return checkpointStore.load(tenantId, runId)
                .orElseThrow(() -> new AgentRunNotFoundException(runId));
    }

    private boolean isTerminal(AgentRunStatus status) {
        return status == AgentRunStatus.COMPLETED || status == AgentRunStatus.COMPENSATED;
    }

    private void ensureSameRequest(AgentRun existing,
                                   String userId,
                                   String orderId,
                                   String newStatus) {
        if (!Objects.equals(existing.userId(), userId)
                || !Objects.equals(existing.orderId(), orderId)
                || !Objects.equals(existing.newStatus(), newStatus)) {
            throw new IllegalArgumentException("同一个幂等键不能用于不同业务请求");
        }
    }

    private void audit(AgentRun run, String action, String result, AuditStatus status) {
        auditLog.record(
                new SecuritySubject(run.userId(), run.tenantId(), Set.of("AGENT_WORKFLOW")),
                action,
                "runId=" + run.runId() + ", orderId=" + run.orderId(),
                result == null ? "" : result,
                status
        );
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
    }
}
