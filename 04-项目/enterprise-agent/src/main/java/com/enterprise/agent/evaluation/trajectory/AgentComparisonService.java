package com.enterprise.agent.evaluation.trajectory;

import java.util.List;

/**
 * 在相同用例和运行次数下比较 Single-Agent 与 Multi-Agent。
 */
public class AgentComparisonService {

    private static final int MIN_RUNS = 5;
    private final StabilityAnalyzer stabilityAnalyzer = new StabilityAnalyzer();

    public AgentComparisonReport compare(List<AgentRunSample> singleSamples,
                                         List<AgentRunSample> multiSamples) {
        if (singleSamples.size() < MIN_RUNS || multiSamples.size() < MIN_RUNS) {
            throw new IllegalArgumentException("每个方案至少运行 5 次才能比较稳定性");
        }
        StabilityReport single = stabilityAnalyzer.analyze(singleSamples);
        StabilityReport multi = stabilityAnalyzer.analyze(multiSamples);
        if (!single.caseId().equals(multi.caseId()) || single.runs() != multi.runs()) {
            throw new IllegalArgumentException("两个方案必须使用相同用例和相同运行次数");
        }

        String recommendation;
        String reason;
        if (dominates(multi, single)) {
            recommendation = "MULTI_AGENT";
            reason = "本次样本中 Multi-Agent 在成功率、成本、P95 延迟和步骤数上 Pareto 支配 Single-Agent";
        } else if (dominates(single, multi)) {
            recommendation = "SINGLE_AGENT";
            reason = "本次样本中 Single-Agent 在成功率、成本、P95 延迟和步骤数上 Pareto 支配 Multi-Agent";
        } else {
            recommendation = "REVIEW_TRADE_OFF";
            reason = "质量、成本和延迟之间存在取舍，需要业务负责人决定";
        }
        return new AgentComparisonReport(
                single,
                multi,
                multi.successRate() - single.successRate(),
                multi.averageCostUsd() - single.averageCostUsd(),
                multi.p95LatencyMillis() - single.p95LatencyMillis(),
                multi.averageStepCount() - single.averageStepCount(),
                recommendation,
                reason);
    }

    private boolean dominates(StabilityReport candidate, StabilityReport other) {
        boolean neverWorse = candidate.successRate() >= other.successRate()
                && candidate.averageCostUsd() <= other.averageCostUsd()
                && candidate.p95LatencyMillis() <= other.p95LatencyMillis()
                && candidate.averageStepCount() <= other.averageStepCount();
        boolean betterSomewhere = candidate.successRate() > other.successRate()
                || candidate.averageCostUsd() < other.averageCostUsd()
                || candidate.p95LatencyMillis() < other.p95LatencyMillis()
                || candidate.averageStepCount() < other.averageStepCount();
        return neverWorse && betterSomewhere;
    }
}
