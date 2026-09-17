package com.enterprise.agent.security.mcp;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;
import com.enterprise.agent.security.agentic.AgenticContentGuard;
import com.enterprise.agent.security.agentic.AgenticSecurityBlockedException;
import com.enterprise.agent.security.agentic.ContentInspection;
import com.enterprise.agent.security.agentic.UntrustedContent;
import com.enterprise.agent.security.agentic.UntrustedContentSource;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolExecutionResult;

import java.util.function.Function;

/**
 * 把第三方 MCP Tool 的运行结果当作不可信数据检查。
 * 静态 Manifest 只能证明“声明没变”，本类负责阻断声明正常但返回隐藏指令的运行时投毒。
 */
public class McpToolOutputGuard {

    private final AgenticContentGuard contentGuard;
    private final AuditLogService auditLog;

    public McpToolOutputGuard(AgenticContentGuard contentGuard,
                              AuditLogService auditLog) {
        this.contentGuard = contentGuard;
        this.auditLog = auditLog;
    }

    /**
     * 返回 {@code Function<ToolExecutor, ToolExecutor>}，可直接传给
     * {@code McpToolProvider.builder().toolWrapper(...)}。外层执行器先调用真实 MCP Tool，
     * 再检查返回文本；命中隐藏指令时抛错，结果不会被送回模型。
     */
    public Function<ToolExecutor, ToolExecutor> wrapperFor(SecuritySubject subject) {
        return delegate -> new ToolExecutor() {
            @Override
            public String execute(ToolExecutionRequest request, Object memoryId) {
                String output = delegate.execute(request, memoryId);
                requireSafe(subject, request, output);
                return output;
            }

            @Override
            public ToolExecutionResult executeWithContext(ToolExecutionRequest request,
                                                          InvocationContext context) {
                ToolExecutionResult result = delegate.executeWithContext(request, context);
                requireSafe(subject, request, result.resultText());
                // 原样返回，保留 isError、structured contents、attributes 等 LangChain4j 上下文。
                return result;
            }
        };
    }

    private void requireSafe(SecuritySubject subject,
                             ToolExecutionRequest request,
                             String output) {
        ContentInspection inspection = contentGuard.inspect(new UntrustedContent(
                UntrustedContentSource.TOOL_OUTPUT,
                request.name(),
                output));
        if (!inspection.safe()) {
            auditLog.record(subject, "mcpOutputGuard:" + request.name(),
                    "tool=" + request.name(), inspection.reason(), AuditStatus.BLOCKED);
            throw new AgenticSecurityBlockedException(inspection.reason(), inspection.risks());
        }
    }
}
