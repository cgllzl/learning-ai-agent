package com.enterprise.agent.evaluation.trajectory;

public record StabilityReport(
        String approach,
        String caseId,
        int runs,
        int passedRuns,
        double successRate,
        double successSampleVariance,
        double averageCostUsd,
        double costSampleStdDev,
        double averageLatencyMillis,
        double latencySampleStdDev,
        long p95LatencyMillis,
        double averageStepCount,
        double averageTokens
) {
}
