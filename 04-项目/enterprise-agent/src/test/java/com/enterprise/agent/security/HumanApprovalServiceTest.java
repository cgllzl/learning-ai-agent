package com.enterprise.agent.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HumanApprovalServiceTest {

    private final HumanApprovalService approvals = new HumanApprovalService();

    @Test
    void highRiskActionRequiresApprovalBeforeItIsApproved() {
        assertThat(approvals.isApproved("O1003", "SHIPPED")).isFalse();

        HumanApprovalService.PendingApproval approval = approvals.requestApproval("O1003", "SHIPPED");

        assertThat(approval.approvalId()).isNotBlank();
        assertThat(approvals.isApproved("O1003", "SHIPPED")).isFalse();
        assertThat(approvals.pendingCount()).isEqualTo(1);

        approvals.approve("O1003", "SHIPPED");

        assertThat(approvals.isApproved("O1003", "SHIPPED")).isTrue();
        assertThat(approvals.pendingCount()).isZero();
    }
}
