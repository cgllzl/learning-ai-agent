package com.enterprise.agent.workflow.durable;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowApprovalServiceTest {

    @Test
    void repeatedRequestReturnsSameApproval() {
        WorkflowApprovalService approvals = new WorkflowApprovalService(new AuditLogService());

        var first = approvals.request("tenant-a", "run-1", "修改订单");
        var second = approvals.request("tenant-a", "run-1", "修改订单");

        assertThat(second.approvalId()).isEqualTo(first.approvalId());
    }

    @Test
    void approvalCannotCrossTenantBoundary() {
        AuditLogService audit = new AuditLogService();
        WorkflowApprovalService approvals = new WorkflowApprovalService(audit);
        approvals.request("tenant-a", "run-1", "修改订单");

        assertThatThrownBy(() -> approvals.approve(subject("tenant-b", "ORDER_ADMIN"), "run-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(approvals.isApproved("tenant-a", "run-1")).isFalse();

        approvals.approve(subject("tenant-a", "ORDER_ADMIN"), "run-1");
        assertThat(approvals.isApproved("tenant-a", "run-1")).isTrue();
        assertThat(audit.entries())
                .extracting(entry -> entry.status())
                .containsExactly(AuditStatus.BLOCKED, AuditStatus.SUCCESS);
    }

    @Test
    void employeeCannotApproveHighRiskWorkflow() {
        AuditLogService audit = new AuditLogService();
        WorkflowApprovalService approvals = new WorkflowApprovalService(audit);
        approvals.request("tenant-a", "run-1", "修改订单");

        assertThatThrownBy(() -> approvals.approve(subject("tenant-a", "EMPLOYEE"), "run-1"))
                .isInstanceOf(SecurityException.class);
        assertThat(approvals.isApproved("tenant-a", "run-1")).isFalse();
        assertThat(audit.entries()).singleElement()
                .satisfies(entry -> assertThat(entry.status()).isEqualTo(AuditStatus.BLOCKED));
    }

    private SecuritySubject subject(String tenantId, String role) {
        return new SecuritySubject("approver-1", tenantId, Set.of(role));
    }
}
