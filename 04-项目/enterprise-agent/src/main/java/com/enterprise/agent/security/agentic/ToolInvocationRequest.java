package com.enterprise.agent.security.agentic;

import java.util.List;
import java.util.Map;

/**
 * 模型提出的 Tool 调用只是候选动作，必须经过 Intent Gate 才能执行。
 */
public record ToolInvocationRequest(
        String toolName,
        String requestedScope,
        String targetHost,
        Map<String, String> arguments,
        List<String> previousTools,
        int toolCallsSoFar,
        boolean humanApproved
) {

    public ToolInvocationRequest {
        arguments = Map.copyOf(arguments);
        previousTools = List.copyOf(previousTools);
    }
}
