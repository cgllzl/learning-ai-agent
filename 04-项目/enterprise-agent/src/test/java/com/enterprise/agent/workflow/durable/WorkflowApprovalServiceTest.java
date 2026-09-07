package com.enterprise.agent.workflow.durable;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowApprovalServiceTest {

    @Test
    void repeatedRequestReturnsSameApproval() {
        WorkflowApprovalService approvals = new WorkflowApprovalService();

        var first = approvals.request("tenant-a", "run-1", "修改订单");
        var second = approvals.request("tenant-a", "run-1", "修改订单");

        assertThat(second.approvalId()).isEqualTo(first.approvalId());
    }

    @Test
    void approvalCannotCrossTenantBoundary() {
        WorkflowApprovalService approvals = new WorkflowApprovalService();
        approvals.request("tenant-a", "run-1", "修改订单");

        assertThatThrownBy(() -> approvals.approve("tenant-b", "run-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(approvals.isApproved("tenant-a", "run-1")).isFalse();

        approvals.approve("tenant-a", "run-1");
        assertThat(approvals.isApproved("tenant-a", "run-1")).isTrue();
    }
}
