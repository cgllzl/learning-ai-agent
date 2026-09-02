package com.enterprise.agent.observability;

/**
 * 一次模型调用的用量指标：Token、延迟、成本。
 */
public record UsageMetrics(
        String requestId,
        long durationMillis,
        int inputTokens,
        int outputTokens,
        int totalTokens,
        double costUsd) {
}
