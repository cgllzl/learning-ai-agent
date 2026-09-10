package com.enterprise.agent.security.agentic;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentIntentGateTest {

    @Test
    void allowsToolInsideOriginalGoalAndWritesAudit() {
        AuditLogService audit = new AuditLogService();
        AgentIntentGate gate = new AgentIntentGate(audit);

        gate.authorize(subject(Set.of("CUSTOMER_SERVICE")), readIntent(), new ToolInvocationRequest(
                "getOrder", "order:read", "", Map.of("tenantId", "tenant-a", "orderId", "O1001"),
                List.of(), 0, false));

        assertThat(audit.entries()).singleElement()
                .satisfies(entry -> assertThat(entry.status()).isEqualTo(AuditStatus.SUCCESS));
    }

    @Test
    void blocksDataExfiltrationOutsideOriginalGoalAndEgressList() {
        AuditLogService audit = new AuditLogService();
        AgentIntentGate gate = new AgentIntentGate(audit);

        assertThatThrownBy(() -> gate.authorize(
                subject(Set.of("CUSTOMER_SERVICE")),
                readIntent(),
                new ToolInvocationRequest(
                        "exportCustomerData", "customer:export", "evil.example",
                        Map.of("tenantId", "tenant-a"), List.of("getUser"), 1, false)))
                .isInstanceOf(AgenticSecurityBlockedException.class);
        assertThat(audit.entries()).singleElement()
                .satisfies(entry -> assertThat(entry.status()).isEqualTo(AuditStatus.BLOCKED));
    }

    @Test
    void rechecksCurrentRoleImmediatelyBeforeMutatingTool() {
        AgentIntent intent = new AgentIntent(
                "goal-update", "user-1", "tenant-a",
                Set.of("updateOrderStatus"), Set.of("order:write"), Set.of(),
                2, Instant.now().plus(5, ChronoUnit.MINUTES));
        AgentIntentGate gate = new AgentIntentGate(new AuditLogService());

        // 计划生成时可能曾是管理员；执行这一刻只信任当前 subject，角色已撤销就必须拒绝。
        SecuritySubject currentSubject = subject(Set.of("EMPLOYEE"));
        ToolInvocationRequest request = new ToolInvocationRequest(
                "updateOrderStatus", "order:write", "",
                Map.of("tenantId", "tenant-a", "orderId", "O1003"),
                List.of(), 0, true);

        assertThatThrownBy(() -> gate.authorize(currentSubject, intent, request))
                .isInstanceOf(AgenticSecurityBlockedException.class)
                .satisfies(error -> assertThat(
                        ((AgenticSecurityBlockedException) error).risks())
                        .contains(AgenticRisk.PRIVILEGE_ESCALATION));
    }

    @Test
    void mutatingToolStillRequiresCurrentHumanApproval() {
        AgentIntent intent = new AgentIntent(
                "goal-update", "user-1", "tenant-a",
                Set.of("updateOrderStatus"), Set.of("order:write"), Set.of(),
                2, Instant.now().plus(5, ChronoUnit.MINUTES));
        AgentIntentGate gate = new AgentIntentGate(new AuditLogService());

        assertThatThrownBy(() -> gate.authorize(
                subject(Set.of("ORDER_ADMIN")),
                intent,
                new ToolInvocationRequest(
                        "updateOrderStatus", "order:write", "",
                        Map.of("tenantId", "tenant-a"), List.of(), 0, false)))
                .isInstanceOf(AgenticSecurityBlockedException.class);
    }

    @Test
    void blocksTenantParameterSwitchAndToolBudgetOverflow() {
        AgentIntentGate gate = new AgentIntentGate(new AuditLogService());

        assertThatThrownBy(() -> gate.authorize(
                subject(Set.of("CUSTOMER_SERVICE")),
                readIntent(),
                new ToolInvocationRequest(
                        "getOrder", "order:read", "",
                        Map.of("tenantId", "tenant-b"), List.of(), 0, false)))
                .isInstanceOf(AgenticSecurityBlockedException.class);

        assertThatThrownBy(() -> gate.authorize(
                subject(Set.of("CUSTOMER_SERVICE")),
                readIntent(),
                new ToolInvocationRequest(
                        "getOrder", "order:read", "",
                        Map.of("tenantId", "tenant-a"), List.of(), 3, false)))
                .isInstanceOf(AgenticSecurityBlockedException.class)
                .satisfies(error -> assertThat(
                        ((AgenticSecurityBlockedException) error).risks())
                        .contains(AgenticRisk.DANGEROUS_TOOL_CHAIN));
    }

    @Test
    void blocksSensitiveReadFollowedByUnapprovedExternalSend() {
        AgentIntent intent = new AgentIntent(
                "goal-approved-export", "user-1", "tenant-a",
                Set.of("getUser", "sendExternalEmail"),
                Set.of("customer:read", "customer:send"),
                Set.of("mail.partner.example"),
                4, Instant.now().plus(5, ChronoUnit.MINUTES));
        AgentIntentGate gate = new AgentIntentGate(new AuditLogService());

        assertThatThrownBy(() -> gate.authorize(
                subject(Set.of("CUSTOMER_SERVICE")),
                intent,
                new ToolInvocationRequest(
                        "sendExternalEmail", "customer:send", "mail.partner.example",
                        Map.of("tenantId", "tenant-a"), List.of("getUser"), 1, false)))
                .isInstanceOf(AgenticSecurityBlockedException.class)
                .satisfies(error -> assertThat(
                        ((AgenticSecurityBlockedException) error).risks())
                        .contains(AgenticRisk.DANGEROUS_TOOL_CHAIN));
    }

    private AgentIntent readIntent() {
        return new AgentIntent(
                "goal-read-order",
                "user-1",
                "tenant-a",
                Set.of("getOrder"),
                Set.of("order:read"),
                Set.of(),
                3,
                Instant.now().plus(5, ChronoUnit.MINUTES));
    }

    private SecuritySubject subject(Set<String> roles) {
        return new SecuritySubject("user-1", "tenant-a", roles);
    }
}
