package com.enterprise.agent.workflow.durable;

public class AgentRunNotFoundException extends RuntimeException {

    public AgentRunNotFoundException(String runId) {
        super("未找到当前租户的 Agent Run：" + runId);
    }
}
