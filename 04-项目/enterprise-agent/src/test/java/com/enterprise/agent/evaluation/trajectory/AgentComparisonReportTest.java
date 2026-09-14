package com.enterprise.agent.evaluation.trajectory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentComparisonReportTest {

    @Test
    void calculatesSuccessVarianceAverageCostAndNearestRankP95() {
        StabilityReport report = new StabilityAnalyzer().analyze(List.of(
                sample("SINGLE", true, 100, 0.001, 2, 100),
                sample("SINGLE", true, 200, 0.002, 2, 120),
                sample("SINGLE", false, 300, 0.003, 3, 140),
                sample("SINGLE", true, 400, 0.004, 2, 160),
                sample("SINGLE", true, 500, 0.005, 3, 180)));

        assertThat(report.runs()).isEqualTo(5);
        assertThat(report.passedRuns()).isEqualTo(4);
        assertThat(report.successRate()).isEqualTo(0.8);
        assertThat(report.successSampleVariance()).isEqualTo(0.2);
        assertThat(report.averageCostUsd()).isEqualTo(0.003);
        assertThat(report.averageLatencyMillis()).isEqualTo(300.0);
        // 5 个样本的 nearest-rank P95 是第 ceil(4.75)=5 个，也就是最大值。
        assertThat(report.p95LatencyMillis()).isEqualTo(500);
        assertThat(report.averageStepCount()).isEqualTo(2.4);
        assertThat(report.averageTokens()).isEqualTo(140.0);
    }

    @Test
    void recommendsSingleAgentWhenQualityIsEqualButMultiCostsMore() {
        List<AgentRunSample> single = repeated("SINGLE", true, 100, 0.001, 2, 100);
        List<AgentRunSample> multi = repeated("MULTI", true, 180, 0.003, 4, 220);

        AgentComparisonReport report = new AgentComparisonService().compare(single, multi);

        assertThat(report.recommendation()).isEqualTo("SINGLE_AGENT");
        assertThat(report.reason()).contains("Pareto 支配");
        assertThat(report.averageCostDeltaUsd()).isPositive();
        assertThat(report.p95LatencyDeltaMillis()).isPositive();
    }

    @Test
    void recommendsMultiAgentOnlyWhenQualityGainFitsCostAndLatencyBudget() {
        List<AgentRunSample> single = List.of(
                sample("SINGLE", true, 100, 0.001, 2, 100),
                sample("SINGLE", true, 100, 0.001, 2, 100),
                sample("SINGLE", true, 100, 0.001, 2, 100),
                sample("SINGLE", true, 100, 0.001, 2, 100),
                sample("SINGLE", false, 100, 0.001, 2, 100));
        List<AgentRunSample> multi = repeated("MULTI", true, 90, 0.0009, 2, 90);

        AgentComparisonReport report = new AgentComparisonService().compare(single, multi);

        assertThat(report.recommendation()).isEqualTo("MULTI_AGENT");
    }

    @Test
    void returnsTradeOffWhenQualityGainRequiresTooMuchCost() {
        List<AgentRunSample> single = List.of(
                sample("SINGLE", true, 100, 0.001, 2, 100),
                sample("SINGLE", true, 100, 0.001, 2, 100),
                sample("SINGLE", true, 100, 0.001, 2, 100),
                sample("SINGLE", true, 100, 0.001, 2, 100),
                sample("SINGLE", false, 100, 0.001, 2, 100));
        List<AgentRunSample> multi = repeated("MULTI", true, 400, 0.010, 8, 500);

        AgentComparisonReport report = new AgentComparisonService().compare(single, multi);

        assertThat(report.recommendation()).isEqualTo("REVIEW_TRADE_OFF");
    }

    @Test
    void rejectsTooFewRunsOrDifferentCases() {
        AgentComparisonService service = new AgentComparisonService();
        assertThatThrownBy(() -> service.compare(
                repeated("SINGLE", true, 100, 0.001, 2, 100).subList(0, 4),
                repeated("MULTI", true, 100, 0.001, 3, 150)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("至少运行 5 次");

        List<AgentRunSample> anotherCase = List.of(
                new AgentRunSample("MULTI", "OTHER_CASE", true, 100, 0.001, 3, 150),
                new AgentRunSample("MULTI", "OTHER_CASE", true, 100, 0.001, 3, 150),
                new AgentRunSample("MULTI", "OTHER_CASE", true, 100, 0.001, 3, 150),
                new AgentRunSample("MULTI", "OTHER_CASE", true, 100, 0.001, 3, 150),
                new AgentRunSample("MULTI", "OTHER_CASE", true, 100, 0.001, 3, 150));
        assertThatThrownBy(() -> service.compare(
                repeated("SINGLE", true, 100, 0.001, 2, 100), anotherCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("相同用例");
    }

    private List<AgentRunSample> repeated(String approach,
                                          boolean passed,
                                          long latency,
                                          double cost,
                                          int steps,
                                          int tokens) {
        return java.util.stream.IntStream.range(0, 5)
                .mapToObj(ignored -> sample(approach, passed, latency, cost, steps, tokens))
                .toList();
    }

    private AgentRunSample sample(String approach,
                                  boolean passed,
                                  long latency,
                                  double cost,
                                  int steps,
                                  int tokens) {
        return new AgentRunSample(
                approach, "HOLDOUT_ORDER_QUERY_O1002", passed,
                latency, cost, steps, tokens);
    }
}
