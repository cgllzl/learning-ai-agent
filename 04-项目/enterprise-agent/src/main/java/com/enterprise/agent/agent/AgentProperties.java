package com.enterprise.agent.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent")
public record AgentProperties(Integer maxSequentialToolsInvocations) {

    public AgentProperties {
        // 防止死循环的关键上限：默认 3 次连续工具调用
        if (maxSequentialToolsInvocations == null || maxSequentialToolsInvocations < 1) {
            maxSequentialToolsInvocations = 3;
        }
    }
}