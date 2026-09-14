package com.enterprise.agent.evaluation.trajectory;

import java.util.Comparator;
import java.util.List;

/**
 * 一次成功像一次考试及格；重复运行统计才像整学期成绩。
 * 本类同时给出平均表现、波动程度和慢请求表现，避免只看一个平均值。
 */
public class StabilityAnalyzer {

    /** 汇总同一方案在同一道题上的多次运行；不同方案或题目不能混算。 */
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
        // 把成功/失败映射成 1/0；方差越大，说明同一道题越容易“时好时坏”。
        // 方差必须和成功率一起看：方差为 0 也可能代表每次都失败。
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
        // nearest-rank P95 取排序后的第 ceil(n×0.95) 个；Java 下标从 0 开始，所以减 1。
        // n=5 时会取第 5 个，也就是最大值，因此这里只是小样本教学演示。
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
            // 单个样本无法估计波动；学习版返回 0，但不应把它解读为“系统绝对稳定”。
            return 0.0;
        }
        double squaredDeviationSum = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum();
        // 样本方差使用 n-1 作分母，修正用有限样本估计总体波动时的系统性偏小。
        return squaredDeviationSum / (values.size() - 1);
    }
}
