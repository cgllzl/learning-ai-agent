package com.enterprise.agent.observability;

import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsageMetricsServiceTest {

    @Test
    void recordsAndSummarizesUsage() {
        UsageMetricsService service = new UsageMetricsService(new CostCalculator(1.0, 1.0));

        service.record(100, new TokenUsage(100, 50, 150));
        service.record(200, new TokenUsage(200, 100, 300));

        UsageSummary summary = service.summary();
        assertThat(summary.totalRequests()).isEqualTo(2);
        assertThat(summary.totalInputTokens()).isEqualTo(300);
        assertThat(summary.totalOutputTokens()).isEqualTo(150);
        assertThat(summary.totalTokens()).isEqualTo(450);
        assertThat(summary.totalCostUsd()).isGreaterThan(0.0);
        assertThat(summary.averageDurationMillis()).isEqualTo(150.0);
    }

    @Test
    void handlesNullTokenUsage() {
        UsageMetricsService service = new UsageMetricsService(new CostCalculator(1.0, 1.0));

        UsageMetrics metric = service.record(10, null);

        assertThat(metric.totalTokens()).isZero();
        assertThat(metric.costUsd()).isZero();
    }
}
