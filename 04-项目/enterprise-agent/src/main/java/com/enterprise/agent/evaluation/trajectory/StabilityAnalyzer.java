package com.enterprise.agent.evaluation.trajectory;

import java.util.Comparator;
import java.util.List;

/**
 * 一次成功像一次考试及格；重复运行统计才像整学期成绩。
 */
public class StabilityAnalyzer {

    public StabilityReport analyze(List<AgentRunSample> samples) {
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("稳定性分析至少需要一条运行样本");
        }
        String approach = samples.getFirst().approach();
        String caseId = samples.getFirst().caseId();
        if (samples.stream().anyMatch(sample -> !approach.equals(sample.approach())
                || !caseId.equals(sample.caseId()))) {
            throw new IllegalArgumentException("一次稳定性统计只能包含同一方案和同一用例");
        }

        int passed = (int) samples.stream().filter(AgentRunSample::passed).count();
        double successRate = passed / (double) samples.size();
        double averageCost = samples.stream().mapToDouble(AgentRunSample::costUsd).average().orElse(0.0);
        double averageLatency = samples.stream().mapToLong(AgentRunSample::latencyMillis).average().orElse(0.0);
        double averageSteps = samples.stream().mapToInt(AgentRunSample::stepCount).average().orElse(0.0);
        double averageTokens = samples.stream().mapToInt(AgentRunSample::totalTokens).average().orElse(0.0);
        double successVariance = sampleVariance(
                samples.stream().map(sample -> sample.passed() ? 1.0 : 0.0).toList(),
                successRate);
        double costStdDev = Math.sqrt(sampleVariance(
                samples.stream().map(AgentRunSample::costUsd).toList(), averageCost));
        double latencyStdDev = Math.sqrt(sampleVariance(
                samples.stream().map(sample -> (double) sample.latencyMillis()).toList(),
                averageLatency));
        List<Long> sortedLatencies = samples.stream()
                .map(AgentRunSample::latencyMillis)
                .sorted(Comparator.naturalOrder())
                .toList();
        int p95Index = Math.max(0, (int) Math.ceil(sortedLatencies.size() * 0.95) - 1);

        return new StabilityReport(
                approach,
                caseId,
                samples.size(),
                passed,
                successRate,
                successVariance,
                averageCost,
                costStdDev,
                averageLatency,
                latencyStdDev,
                sortedLatencies.get(p95Index),
                averageSteps,
                averageTokens
        );
    }

    private double sampleVariance(List<Double> values, double mean) {
        if (values.size() < 2) {
            return 0.0;
        }
        double squaredDeviationSum = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum();
        return squaredDeviationSum / (values.size() - 1);
    }
}
