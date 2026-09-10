package com.enterprise.agent.security.agentic;

import java.time.Instant;
import java.util.Set;

/**
 * 用户原始目标的“不可变任务边界”。后续文档、Tool 输出或子 Agent 无权扩大它。
 */
public record AgentIntent(
        String goalId,
        String userId,
        String tenantId,
        Set<String> allowedTools,
        Set<String> allowedScopes,
        Set<String> allowedEgressHosts,
        int maxToolCalls,
        Instant expiresAt
) {

    public AgentIntent {
        allowedTools = Set.copyOf(allowedTools);
        allowedScopes = Set.copyOf(allowedScopes);
        allowedEgressHosts = Set.copyOf(allowedEgressHosts);
        if (maxToolCalls < 1) {
            throw new IllegalArgumentException("maxToolCalls 必须大于 0");
        }
    }
}
