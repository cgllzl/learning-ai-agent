package com.enterprise.agent.evaluation;

import com.enterprise.agent.rag.RetrievedChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentEvaluationServiceTest {

    private final AgentEvaluationService evaluator = new AgentEvaluationService();

    @Test
    void correctnessPassesWhenAllExpectedChecksArePresent() {
        AgentEvalCase evalCase = AgentEvalCaseCatalog.byId("ORDER_QUERY");

        AgentEvalResult result = evaluator.evaluateCorrectness(
                evalCase, "订单 O1001 金额 399 元");

        assertThat(result.passed()).isTrue();
    }

    @Test
    void correctnessFailsWhenSomeExpectedChecksAreMissing() {
        AgentEvalCase evalCase = AgentEvalCaseCatalog.byId("ORDER_QUERY");

        AgentEvalResult result = evaluator.evaluateCorrectness(
                evalCase, "订单 O1001 的信息");

        assertThat(result.passed()).isFalse();
        assertThat(result.detail()).contains("399");
    }

    @Test
    void citationAccuracyPassesOnlyWhenCitationPointsToExpectedDocument() {
        AgentEvalCase evalCase = AgentEvalCaseCatalog.byId("RAG_CITATION");
        List<RetrievedChunk> sources = List.of(new RetrievedChunk("满一年5天年假", 0.9, "HR-001"));

        AgentEvalResult good = evaluator.evaluateCitationAccuracy(evalCase, "年假 5 天[1]", sources, "HR-001");
        AgentEvalResult bad = evaluator.evaluateCitationAccuracy(evalCase, "年假 5 天[2]", sources, "HR-001");
        AgentEvalResult missing = evaluator.evaluateCitationAccuracy(evalCase, "年假 5 天", sources, "HR-001");

        assertThat(good.passed()).isTrue();
        assertThat(bad.passed()).isFalse();
        assertThat(missing.passed()).isFalse();
    }
}
