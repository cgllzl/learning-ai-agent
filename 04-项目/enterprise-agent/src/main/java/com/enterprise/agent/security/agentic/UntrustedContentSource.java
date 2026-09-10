package com.enterprise.agent.security.agentic;

/**
 * 所有进入模型上下文的外部内容都要标注来源，不能因为它来自内部链路就自动信任。
 */
public enum UntrustedContentSource {
    USER_INPUT,
    RAG_DOCUMENT,
    MEMORY,
    TOOL_OUTPUT,
    AGENT_MESSAGE,
    MCP_DESCRIPTOR
}
