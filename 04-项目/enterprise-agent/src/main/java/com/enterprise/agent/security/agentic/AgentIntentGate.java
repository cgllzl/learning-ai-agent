package com.enterprise.agent.security.agentic;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;
import com.enterprise.agent.security.ToolPermission;
import com.enterprise.agent.security.ToolPermissionCatalog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/**
 * Tool 执行前的最后一道闸门：每次都重新校验当前身份、目标、Scope、参数、出站和预算。
 */
@Component
public class AgentIntentGate {

    private static final Set<String> SENSITIVE_READ_TOOLS = Set.of(
            "getOrder", "getUser", "readCustomerData", "searchCustomerData");
    private static final Set<String> EXTERNAL_SEND_TOOLS = Set.of(
            "sendExternalEmail", "exportCustomerData", "uploadExternalFile");

    private final AuditLogService auditLog;

    public AgentIntentGate(AuditLogService auditLog) {
        this.auditLog = auditLog;
    }

    public void authorize(SecuritySubject currentSubject,
                          AgentIntent intent,
                          ToolInvocationRequest request) {
        if (!intent.userId().equals(currentSubject.userId())
                || !intent.tenantId().equals(currentSubject.tenantId())) {
            block(currentSubject, intent, request, AgenticRisk.IDENTITY_MISMATCH,
                    "当前身份与原始任务身份不一致");
        }
        if (Instant.now().isAfter(intent.expiresAt())) {
            block(currentSubject, intent, request, AgenticRisk.EXPIRED_MESSAGE,
                    "原始任务授权已过期");
        }
        if (request.toolCallsSoFar() >= intent.maxToolCalls()) {
            block(currentSubject, intent, request, AgenticRisk.DANGEROUS_TOOL_CHAIN,
                    "Tool 调用次数达到任务预算上限");
        }
        if (!intent.allowedTools().contains(request.toolName())) {
            block(currentSubject, intent, request, AgenticRisk.SCOPE_VIOLATION,
                    "Tool 不在原始目标允许列表中");
        }
        if (!intent.allowedScopes().contains(request.requestedScope())) {
            block(currentSubject, intent, request, AgenticRisk.SCOPE_VIOLATION,
                    "请求的数据 Scope 超出原始授权");
        }

        String argumentTenant = request.arguments().get("tenantId");
        if (argumentTenant != null && !intent.tenantId().equals(argumentTenant)) {
            block(currentSubject, intent, request, AgenticRisk.IDENTITY_MISMATCH,
                    "Tool 参数试图切换租户");
        }
        if (request.targetHost() != null && !request.targetHost().isBlank()
                && !intent.allowedEgressHosts().contains(request.targetHost())) {
            block(currentSubject, intent, request, AgenticRisk.EGRESS_VIOLATION,
                    "目标主机不在出站允许列表中");
        }
        if (containsSensitiveRead(request.previousTools())
                && EXTERNAL_SEND_TOOLS.contains(request.toolName())
                && !request.humanApproved()) {
            block(currentSubject, intent, request, AgenticRisk.DANGEROUS_TOOL_CHAIN,
                    "敏感读取后外发数据需要单独人工确认");
        }

        ToolPermissionCatalog.find(request.toolName()).ifPresent(permission ->
                recheckCurrentPermission(currentSubject, intent, request, permission));

        auditLog.record(
                currentSubject,
                "intentGate:" + request.toolName(),
                safeArguments(intent, request),
                "ALLOW",
                AuditStatus.SUCCESS
        );
    }

    private void recheckCurrentPermission(SecuritySubject subject,
                                          AgentIntent intent,
                                          ToolInvocationRequest request,
                                          ToolPermission permission) {
        boolean hasCurrentRole = permission.requiredRoles().stream()
                .anyMatch(subject.roles()::contains);
        if (!hasCurrentRole) {
            block(subject, intent, request, AgenticRisk.PRIVILEGE_ESCALATION,
                    "当前角色已无权调用该 Tool");
        }
        if (permission.requiresHumanApproval() && !request.humanApproved()) {
            block(subject, intent, request, AgenticRisk.PRIVILEGE_ESCALATION,
                    "高风险 Tool 缺少当前操作的人工审批");
        }
    }

    private boolean containsSensitiveRead(Iterable<String> tools) {
        for (String tool : tools) {
            if (SENSITIVE_READ_TOOLS.contains(tool)) {
                return true;
            }
        }
        return false;
    }

    private void block(SecuritySubject subject,
                       AgentIntent intent,
                       ToolInvocationRequest request,
                       AgenticRisk risk,
                       String reason) {
        auditLog.record(
                subject,
                "intentGate:" + request.toolName(),
                safeArguments(intent, request),
                reason,
                AuditStatus.BLOCKED
        );
        throw new AgenticSecurityBlockedException(reason, risk);
    }

    private String safeArguments(AgentIntent intent, ToolInvocationRequest request) {
        return "goalId=" + intent.goalId()
                + ", scope=" + request.requestedScope()
                + ", targetHost=" + request.targetHost()
                + ", argumentKeys=" + request.arguments().keySet();
    }
}
