package com.enterprise.agent.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogServiceTest {

    @Test
    void recordsToolInvocationWithSubjectAndStatus() {
        AuditLogService auditLog = new AuditLogService();
        SecuritySubject subject = new SecuritySubject("u1", "t1", Set.of("ORDER_ADMIN"));

        auditLog.record(subject, "updateOrderStatus", "O1003 -> SHIPPED", "等待审批", AuditStatus.PENDING_APPROVAL);

        assertThat(auditLog.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.userId()).isEqualTo("u1");
            assertThat(entry.tenantId()).isEqualTo("t1");
            assertThat(entry.toolName()).isEqualTo("updateOrderStatus");
            assertThat(entry.status()).isEqualTo(AuditStatus.PENDING_APPROVAL);
        });
    }
}
