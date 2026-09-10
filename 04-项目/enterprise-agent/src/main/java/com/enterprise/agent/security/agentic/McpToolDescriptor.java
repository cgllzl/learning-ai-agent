package com.enterprise.agent.security.agentic;

/**
 * 第三方 MCP 实际提供的 Tool 身份与说明。
 */
public record McpToolDescriptor(
        String serverId,
        String toolName,
        String version,
        String description,
        String targetHost
) {
}
