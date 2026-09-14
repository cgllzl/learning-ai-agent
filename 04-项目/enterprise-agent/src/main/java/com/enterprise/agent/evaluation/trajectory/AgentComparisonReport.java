package com.enterprise.agent.evaluation.trajectory;

/**
 * Single-Agent 与 Multi-Agent 的同场对比结果。
 *
 * <p>四个 delta 都固定按 {@code Multi - Single} 计算。成功率差值为正表示 Multi 更高；
 * 成本、延迟、步骤数差值为正，则表示 Multi 付出的代价更大。</p>
 */
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
