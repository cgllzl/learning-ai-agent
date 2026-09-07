package com.enterprise.agent.workflow.durable;

/**
 * 可持久化、可审计的业务步骤。这里记录“做什么”，不记录模型的隐藏思维过程。
 */
public enum AgentStep {
    UPDATE_ORDER,
    SEND_NOTIFICATION
}
