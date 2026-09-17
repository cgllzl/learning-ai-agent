package com.enterprise.agent.security.mcp;

import com.enterprise.agent.security.agentic.AgenticSecurityBlockedException;
import dev.langchain4j.service.tool.ToolErrorContext;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;

/**
 * 防止 AiServices 把安全门禁异常包装成普通 Tool 错误后继续让模型推理。
 * 安全异常必须直接终止；普通技术错误只返回不含内部细节的通用文本。
 */
public class FailClosedMcpToolErrorHandler implements ToolExecutionErrorHandler {

    @Override
    public ToolErrorHandlerResult handle(Throwable error, ToolErrorContext context) {
        if (error instanceof AgenticSecurityBlockedException blocked) {
            throw blocked;
        }
        return ToolErrorHandlerResult.text("MCP 工具执行失败，请稍后重试或转人工处理");
    }
}
