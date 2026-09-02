package com.enterprise.agent.observability;

import dev.langchain4j.model.output.TokenUsage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 记录并汇总模型调用指标（Week 6 Day 4）。
 */
public class UsageMetricsService {

    private final CostCalculator costCalculator;
    private final List<UsageMetrics> metrics = new CopyOnWriteArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong();

    public UsageMetricsService(CostCalculator costCalculator) {
        this.costCalculator = costCalculator;
    }

    public UsageMetrics record(long durationMillis, TokenUsage tokenUsage) {
        int inputTokens = tokenUsage == null || tokenUsage.inputTokenCount() == null
                ? 0 : tokenUsage.inputTokenCount();
        int outputTokens = tokenUsage == null || tokenUsage.outputTokenCount() == null
                ? 0 : tokenUsage.outputTokenCount();
        int totalTokens = tokenUsage == null || tokenUsage.totalTokenCount() == null
                ? inputTokens + outputTokens : tokenUsage.totalTokenCount();
        double costUsd = costCalculator.calculateUsd(inputTokens, outputTokens);

        UsageMetrics metric = new UsageMetrics(
                "req-" + idGenerator.incrementAndGet(),
                durationMillis,
                inputTokens,
                outputTokens,
                totalTokens,
                costUsd);
        metrics.add(metric);
        return metric;
    }

    public UsageSummary summary() {
        int totalInputTokens = metrics.stream().mapToInt(UsageMetrics::inputTokens).sum();
        int totalOutputTokens = metrics.stream().mapToInt(UsageMetrics::outputTokens).sum();
        int totalTokens = metrics.stream().mapToInt(UsageMetrics::totalTokens).sum();
        double totalCostUsd = metrics.stream().mapToDouble(UsageMetrics::costUsd).sum();
        double averageDurationMillis = metrics.isEmpty()
                ? 0.0
                : metrics.stream().mapToLong(UsageMetrics::durationMillis).average().orElse(0.0);
        return new UsageSummary(
                metrics.size(),
                totalInputTokens,
                totalOutputTokens,
                totalTokens,
                totalCostUsd,
                averageDurationMillis);
    }

    public List<UsageMetrics> metrics() {
        return List.copyOf(metrics);
    }
}
