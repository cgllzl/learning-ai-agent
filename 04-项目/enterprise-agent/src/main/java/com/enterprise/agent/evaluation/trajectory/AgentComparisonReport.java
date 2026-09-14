package com.enterprise.agent.evaluation.trajectory;

public record AgentComparisonReport(
        StabilityReport singleAgent,
        StabilityReport multiAgent,
        double successRateDelta,
        double averageCostDeltaUsd,
        long p95LatencyDeltaMillis,
        double averageStepDelta,
        String recommendation,
        String reason
) {
}
