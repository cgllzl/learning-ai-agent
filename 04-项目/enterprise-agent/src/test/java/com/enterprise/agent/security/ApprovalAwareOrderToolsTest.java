package com.enterprise.agent.security;

import com.enterprise.agent.agent.MockOrderData;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalAwareOrderToolsTest {

    @Test
    void updateOrderStatusFirstCreatesApprovalThenExecutesAfterApprove() {
        MockOrderData data = new MockOrderData();
        AuditLogService auditLog = new AuditLogService();
        HumanApprovalService approvals = new HumanApprovalService();
        ApprovalAwareOrderTools tools = new ApprovalAwareOrderTools(data, auditLog, approvals);
        SecuritySubject subject = new SecuritySubject("u1", "t1", Set.of("ORDER_ADMIN"));

        // 第一次调用：需要人工审批，数据不变
        String pendingReply = TenantContext.run(subject, () -> tools.updateOrderStatus("O1003", "SHIPPED"));
        assertThat(pendingReply).contains("人工审批");
        assertThat(data.findOrderById("O1003").orElseThrow().status()).isEqualTo("PENDING");
        assertThat(auditLog.entries()).extracting(AuditLogEntry::status)
                .contains(AuditStatus.PENDING_APPROVAL);

        // 人工审批后再次调用：真正执行
        approvals.approve("O1003", "SHIPPED");
        String successReply = TenantContext.run(subject, () -> tools.updateOrderStatus("O1003", "SHIPPED"));
        assertThat(successReply).contains("已更新为 SHIPPED");
        assertThat(data.findOrderById("O1003").orElseThrow().status()).isEqualTo("SHIPPED");
        assertThat(auditLog.entries()).extracting(AuditLogEntry::status)
                .contains(AuditStatus.SUCCESS);
    }
}
