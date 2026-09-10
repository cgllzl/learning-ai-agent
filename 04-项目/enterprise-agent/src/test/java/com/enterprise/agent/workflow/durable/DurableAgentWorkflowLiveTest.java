package com.enterprise.agent.workflow.durable;

import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.SecuritySubject;
import com.enterprise.agent.security.agentic.AgentIntentGate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Week 6.5 Day 2 真实联调：DeepSeek 生成白名单计划，Workflow 审批后执行，
 * 通知首次失败，使用同一 Checkpoint Store 的新服务实例从失败步骤恢复。
 *
 * 运行：.\scripts\test-live.ps1 -Test DurableAgentWorkflowLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class DurableAgentWorkflowLiveTest {

    @Test
    void realModelPlanSurvivesFailureAndResumeDoesNotRepeatOrderUpdate() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .build();
        InMemoryAgentCheckpointStore store = new InMemoryAgentCheckpointStore();
        IdempotentToolExecutor toolExecutor = new IdempotentToolExecutor();
        AuditLogService audit = new AuditLogService();
        WorkflowApprovalService approvals = new WorkflowApprovalService(audit);
        CountingOrderGateway orderGateway = new CountingOrderGateway(new MockOrderData());
        FailOnceNotificationGateway notificationGateway = new FailOnceNotificationGateway();
        AgentIntentGate intentGate = new AgentIntentGate(audit);
        SecuritySubject subject = new SecuritySubject(
                "U1", "tenant-a", Set.of("ORDER_ADMIN"));

        DurableOrderWorkflowService firstProcess = new DurableOrderWorkflowService(
                model, store, toolExecutor, approvals, orderGateway, notificationGateway, audit, intentGate);

        AgentRun waiting = firstProcess.start(
                subject, "day2-live-order-O1003", "O1003", "SHIPPED");
        System.out.println("[Day2 计划生成] " + waiting);
        AgentRun beforeApproval = firstProcess.resume(subject, waiting.runId());
        assertThat(waiting.status()).isEqualTo(AgentRunStatus.WAITING_APPROVAL);
        assertThat(beforeApproval.status()).isEqualTo(AgentRunStatus.WAITING_APPROVAL);
        assertThat(orderGateway.updateCalls()).isZero();

        approvals.approve(subject, waiting.runId());
        AgentRun notificationFailed = firstProcess.resume(subject, waiting.runId());
        assertThat(notificationFailed.status()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(notificationFailed.completedSteps()).containsExactly(AgentStep.UPDATE_ORDER);
        assertThat(notificationFailed.nextStep()).isEqualTo(AgentStep.SEND_NOTIFICATION);
        assertThat(orderGateway.currentStatus("O1003")).isEqualTo("SHIPPED");

        // 模拟服务对象重建：状态、幂等结果和审批记录仍由外部 Store/组件保存。
        DurableOrderWorkflowService restartedProcess = new DurableOrderWorkflowService(
                model, store, toolExecutor, approvals, orderGateway, notificationGateway, audit, intentGate);
        AgentRun completed = restartedProcess.resume(subject, waiting.runId());
        AgentRun duplicateRequest = restartedProcess.start(
                subject, "day2-live-order-O1003", "O1003", "SHIPPED");

        System.out.println("[Day2 等待审批] " + waiting);
        System.out.println("[Day2 通知失败存档] " + notificationFailed);
        System.out.println("[Day2 恢复完成] " + completed);

        assertThat(completed.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(completed.completedSteps()).containsExactly(
                AgentStep.UPDATE_ORDER, AgentStep.SEND_NOTIFICATION);
        assertThat(duplicateRequest.runId()).isEqualTo(waiting.runId());
        assertThat(orderGateway.updateCalls()).isEqualTo(1);
        assertThat(notificationGateway.attempts()).isEqualTo(2);
        assertThat(notificationGateway.sentCount()).isEqualTo(1);
        assertThat(toolExecutor.executionCount("tenant-a")).isEqualTo(2);
    }

    private static final class CountingOrderGateway implements OrderWorkflowGateway {
        private final MockOrderData orderData;
        private int updateCalls;

        private CountingOrderGateway(MockOrderData orderData) {
            this.orderData = orderData;
        }

        @Override
        public String currentStatus(String orderId) {
            return orderData.findOrderById(orderId).orElseThrow().status();
        }

        @Override
        public String updateStatus(String orderId, String newStatus) {
            updateCalls++;
            orderData.updateOrderStatus(orderId, newStatus);
            return "订单 " + orderId + " 状态已更新为 " + newStatus;
        }

        private int updateCalls() {
            return updateCalls;
        }
    }

    private static final class FailOnceNotificationGateway implements NotificationGateway {
        private int attempts;
        private int sentCount;

        @Override
        public String sendStatusChanged(String userId, String orderId, String newStatus) {
            attempts++;
            if (attempts == 1) {
                throw new IllegalStateException("模拟通知服务第一次调用失败");
            }
            sentCount++;
            return "通知发送成功";
        }

        private int attempts() {
            return attempts;
        }

        private int sentCount() {
            return sentCount;
        }
    }
}
