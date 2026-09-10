package com.enterprise.agent.security.agentic;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Tool 的安全门卫：执行前检查意图，执行后把 Tool 输出当作不可信数据再次检查。
 */
@Component
public class GuardedToolExecutor {

    private final AgentIntentGate intentGate;
    private final AgenticContentGuard contentGuard;
    private final AuditLogService auditLog;

    public GuardedToolExecutor(AgentIntentGate intentGate,
                               AgenticContentGuard contentGuard,
                               AuditLogService auditLog) {
        this.intentGate = intentGate;
        this.contentGuard = contentGuard;
        this.auditLog = auditLog;
    }

    public String execute(SecuritySubject subject,
                          AgentIntent intent,
                          ToolInvocationRequest request,
                          Supplier<String> action) {
        intentGate.authorize(subject, intent, request);
        String output = action.get();
        ContentInspection inspection = contentGuard.inspect(new UntrustedContent(
                UntrustedContentSource.TOOL_OUTPUT,
                request.toolName(),
                output
        ));
        if (!inspection.safe()) {
            auditLog.record(subject, "toolOutputGuard:" + request.toolName(),
                    "goalId=" + intent.goalId(), inspection.reason(), AuditStatus.BLOCKED);
            throw new AgenticSecurityBlockedException(inspection.reason(), inspection.risks());
        }
        auditLog.record(subject, "toolCall:" + request.toolName(),
                "goalId=" + intent.goalId(), "EXECUTED", AuditStatus.SUCCESS);
        return output;
    }
}
