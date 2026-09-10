package com.enterprise.agent.workflow.durable;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;
import com.enterprise.agent.security.agentic.AgentIntentGate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

class DurableAgentWorkflowTest {

    private static final String USER = "user-1";
    private static final String TENANT = "tenant-a";
    private static final String ORDER = "O1003";
    private static final String NEW_STATUS = "SHIPPED";

    @Test
    void pausesBeforeEverySideEffectUntilHumanApproves() {
        Fixture fixture = fixture();

        AgentRun waiting = fixture.workflow.start(subject(TENANT), "idem-1", ORDER, NEW_STATUS);
        AgentRun stillWaiting = fixture.workflow.resume(subject(TENANT), waiting.runId());

        assertThat(waiting.status()).isEqualTo(AgentRunStatus.WAITING_APPROVAL);
        assertThat(waiting.approvalId()).isNotBlank();
        assertThat(stillWaiting.status()).isEqualTo(AgentRunStatus.WAITING_APPROVAL);
        assertThat(stillWaiting.nextStep()).isEqualTo(AgentStep.UPDATE_ORDER);
        verify(fixture.orderGateway, never()).updateStatus(anyString(), anyString());
        verify(fixture.notificationGateway, never())
                .sendStatusChanged(anyString(), anyString(), anyString());
    }

    @Test
    void resumesFromCheckpointWithoutRepeatingCompletedOrderUpdate() {
        Fixture fixture = fixture();
        when(fixture.notificationGateway.sendStatusChanged(USER, ORDER, NEW_STATUS))
                .thenThrow(new IllegalStateException("通知系统暂时不可用"))
                .thenReturn("通知成功");

        AgentRun waiting = fixture.workflow.start(subject(TENANT), "idem-2", ORDER, NEW_STATUS);
        fixture.approvals.approve(subject(TENANT), waiting.runId());

        AgentRun failed = fixture.workflow.resume(subject(TENANT), waiting.runId());
        assertThat(failed.status()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(failed.completedSteps()).containsExactly(AgentStep.UPDATE_ORDER);
        assertThat(failed.nextStep()).isEqualTo(AgentStep.SEND_NOTIFICATION);

        DurableOrderWorkflowService restartedService = fixture.newServiceInstance();
        AgentRun completed = restartedService.resume(subject(TENANT), waiting.runId());

        assertThat(completed.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(completed.completedSteps()).containsExactly(
                AgentStep.UPDATE_ORDER, AgentStep.SEND_NOTIFICATION);
        verify(fixture.orderGateway, times(1)).updateStatus(ORDER, NEW_STATUS);
        verify(fixture.notificationGateway, times(2))
                .sendStatusChanged(USER, ORDER, NEW_STATUS);
        assertThat(fixture.toolExecutor.executionCount(TENANT)).isEqualTo(2);
    }

    @Test
    void duplicateIdempotencyKeyReturnsOriginalRunButRejectsDifferentPayload() {
        Fixture fixture = fixture();

        AgentRun first = fixture.workflow.start(subject(TENANT), "idem-3", ORDER, NEW_STATUS);
        AgentRun duplicate = fixture.workflow.start(subject(TENANT), "idem-3", ORDER, NEW_STATUS);

        assertThat(duplicate.runId()).isEqualTo(first.runId());
        verify(fixture.planner, times(1)).plan(ORDER, NEW_STATUS);

        assertThatThrownBy(() -> fixture.workflow.start(
                subject(TENANT), "idem-3", ORDER, "CANCELLED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("幂等键");
    }

    @Test
    void anotherTenantCannotLoadOrResumeRun() {
        Fixture fixture = fixture();
        AgentRun waiting = fixture.workflow.start(subject(TENANT), "idem-4", ORDER, NEW_STATUS);

        assertThat(fixture.store.load("tenant-b", waiting.runId())).isEmpty();
        assertThatThrownBy(() -> fixture.workflow.resume(subject("tenant-b"), waiting.runId()))
                .isInstanceOf(AgentRunNotFoundException.class);
        verify(fixture.orderGateway, never()).updateStatus(anyString(), anyString());
    }

    @Test
    void roleRevokedWhileWaitingIsBlockedImmediatelyBeforeToolExecution() {
        Fixture fixture = fixture();
        AgentRun waiting = fixture.workflow.start(subject(TENANT), "idem-role-revoked", ORDER, NEW_STATUS);
        fixture.approvals.approve(subject(TENANT), waiting.runId());

        SecuritySubject revoked = new SecuritySubject(USER, TENANT, Set.of("EMPLOYEE"));
        AgentRun blocked = fixture.workflow.resume(revoked, waiting.runId());

        assertThat(blocked.status()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(blocked.lastError()).contains("当前角色已无权");
        assertThat(blocked.completedSteps()).isEmpty();
        verify(fixture.orderGateway, never()).updateStatus(anyString(), anyString());
        assertThat(fixture.audit.entries())
                .anyMatch(entry -> entry.status() == AuditStatus.BLOCKED);
    }

    @Test
    void compensatesOrderUpdateAfterNotificationFailure() {
        Fixture fixture = fixture();
        when(fixture.notificationGateway.sendStatusChanged(USER, ORDER, NEW_STATUS))
                .thenThrow(new IllegalStateException("通知永久失败"));

        AgentRun waiting = fixture.workflow.start(subject(TENANT), "idem-5", ORDER, NEW_STATUS);
        fixture.approvals.approve(subject(TENANT), waiting.runId());
        AgentRun failed = fixture.workflow.resume(subject(TENANT), waiting.runId());
        AgentRun compensated = fixture.workflow.compensate(subject(TENANT), failed.runId());

        assertThat(compensated.status()).isEqualTo(AgentRunStatus.COMPENSATED);
        verify(fixture.orderGateway).updateStatus(ORDER, NEW_STATUS);
        verify(fixture.orderGateway).updateStatus(ORDER, "PENDING");
        assertThat(fixture.audit.entries())
                .anyMatch(entry -> entry.status() == AuditStatus.COMPENSATED);
    }

    @Test
    void recordsCompensationFailureForManualRecovery() {
        Fixture fixture = fixture();
        when(fixture.notificationGateway.sendStatusChanged(USER, ORDER, NEW_STATUS))
                .thenThrow(new IllegalStateException("通知永久失败"));
        when(fixture.orderGateway.updateStatus(ORDER, "PENDING"))
                .thenThrow(new IllegalStateException("订单系统拒绝回滚"));

        AgentRun waiting = fixture.workflow.start(subject(TENANT), "idem-6", ORDER, NEW_STATUS);
        fixture.approvals.approve(subject(TENANT), waiting.runId());
        AgentRun failed = fixture.workflow.resume(subject(TENANT), waiting.runId());
        AgentRun compensationFailed = fixture.workflow.compensate(subject(TENANT), failed.runId());

        assertThat(compensationFailed.status()).isEqualTo(AgentRunStatus.COMPENSATION_FAILED);
        assertThat(compensationFailed.lastError()).contains("拒绝回滚");
        assertThat(fixture.store.load(TENANT, waiting.runId()))
                .map(AgentRun::status)
                .contains(AgentRunStatus.COMPENSATION_FAILED);
    }

    private Fixture fixture() {
        DurableWorkflowPlannerAssistant planner = mock(DurableWorkflowPlannerAssistant.class);
        when(planner.plan(ORDER, NEW_STATUS)).thenReturn("UPDATE_ORDER,SEND_NOTIFICATION");

        InMemoryAgentCheckpointStore store = new InMemoryAgentCheckpointStore();
        IdempotentToolExecutor toolExecutor = new IdempotentToolExecutor();
        AuditLogService audit = new AuditLogService();
        WorkflowApprovalService approvals = new WorkflowApprovalService(audit);
        OrderWorkflowGateway orderGateway = mock(OrderWorkflowGateway.class);
        when(orderGateway.currentStatus(ORDER)).thenReturn("PENDING");
        when(orderGateway.updateStatus(ORDER, NEW_STATUS)).thenReturn("订单更新成功");
        NotificationGateway notificationGateway = mock(NotificationGateway.class);
        when(notificationGateway.sendStatusChanged(USER, ORDER, NEW_STATUS)).thenReturn("通知成功");
        return new Fixture(
                planner,
                store,
                toolExecutor,
                approvals,
                orderGateway,
                notificationGateway,
                audit
        );
    }

    private SecuritySubject subject(String tenantId) {
        return new SecuritySubject(USER, tenantId, Set.of("ORDER_ADMIN"));
    }

    private static final class Fixture {
        private final DurableWorkflowPlannerAssistant planner;
        private final InMemoryAgentCheckpointStore store;
        private final IdempotentToolExecutor toolExecutor;
        private final WorkflowApprovalService approvals;
        private final OrderWorkflowGateway orderGateway;
        private final NotificationGateway notificationGateway;
        private final AuditLogService audit;
        private final DurableOrderWorkflowService workflow;

        private Fixture(DurableWorkflowPlannerAssistant planner,
                        InMemoryAgentCheckpointStore store,
                        IdempotentToolExecutor toolExecutor,
                        WorkflowApprovalService approvals,
                        OrderWorkflowGateway orderGateway,
                        NotificationGateway notificationGateway,
                        AuditLogService audit) {
            this.planner = planner;
            this.store = store;
            this.toolExecutor = toolExecutor;
            this.approvals = approvals;
            this.orderGateway = orderGateway;
            this.notificationGateway = notificationGateway;
            this.audit = audit;
            this.workflow = newServiceInstance();
        }

        private DurableOrderWorkflowService newServiceInstance() {
            return new DurableOrderWorkflowService(
                    planner,
                    store,
                    toolExecutor,
                    approvals,
                    orderGateway,
                    notificationGateway,
                    audit,
                    new AgentIntentGate(audit)
            );
        }
    }
}
