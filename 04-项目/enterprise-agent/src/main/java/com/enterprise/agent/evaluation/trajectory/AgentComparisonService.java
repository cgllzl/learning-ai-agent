package com.enterprise.agent.evaluation.trajectory;

import java.util.List;

/**
 * 在相同用例和运行次数下比较 Single-Agent 与 Multi-Agent。
 *
 * <p>本类只实现 Pareto 比较，不包含具体的 SLO 或预算阈值。
 * 两个方案互有优劣时，不强行凑成一个总分，而是把取舍交给业务负责人。</p>
 */
public class AgentComparisonService {

    // 5 次只是本学习项目允许比较的最低门槛，并不代表已达到生产统计可信度。
    private static final int MIN_RUNS = 5;
    private final StabilityAnalyzer stabilityAnalyzer = new StabilityAnalyzer();

    /**
     * 先分别汇总两组选手的稳定性，再在成功率、成本、P95 延迟和步骤数上比较。
     */
    public AgentComparisonReport compare(List<AgentRunSample> singleSamples,
                                         List<AgentRunSample> multiSamples) {
        if (singleSamples.size() < MIN_RUNS || multiSamples.size() < MIN_RUNS) {
            throw new IllegalArgumentException("每个方案至少运行 5 次才能比较稳定性");
        }
        StabilityReport single = stabilityAnalyzer.analyze(singleSamples);
        StabilityReport multi = stabilityAnalyzer.analyze(multiSamples);
        // 同题、同次数才是公平的 A/B 比较，否则差异可能来自题目或样本量。
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

    /**
     * Pareto 支配：所有指标都不差，并且至少一项严格更好。
     * 成功率越高越好；成本、P95 延迟和步骤数越低越好。
     */
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
