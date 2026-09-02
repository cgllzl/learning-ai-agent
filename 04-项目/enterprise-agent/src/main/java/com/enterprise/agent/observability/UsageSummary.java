package com.enterprise.agent.observability;

/**
 * 汇总后的用量指标。
 */
public record UsageSummary(
        int totalRequests,
        int totalInputTokens,
        int totalOutputTokens,
        int totalTokens,
        double totalCostUsd,
        double averageDurationMillis) {
}
