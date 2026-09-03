package com.enterprise.agent.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvalRegressionRunnerTest {

    private final EvalRegressionRunner runner =
            new EvalRegressionRunner(new AgentEvaluationService());

    @Test
    void weakAnswerFailsAndImprovedAnswerPasses() {
        AgentEvalCase evalCase = AgentEvalCaseCatalog.byId("ORDER_QUERY");

        EvalRunReport failedReport = runner.run(
                List.of(evalCase),
                ignored -> "订单 O1001 的信息");

        assertThat(failedReport.failed()).isEqualTo(1);
        assertThat(failedReport.failures()).singleElement().satisfies(failure -> {
            assertThat(failure.caseId()).isEqualTo("ORDER_QUERY");
            assertThat(failure.reason()).contains("399");
        });

        EvalRunReport improvedReport = runner.run(
                List.of(evalCase),
                ignored -> "订单 O1001 金额 399 元");

        assertThat(improvedReport.passed()).isEqualTo(1);
        assertThat(improvedReport.success()).isTrue();
    }
}
