package com.enterprise.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagEvaluatorTest {

    private static final List<RetrievedChunk> SOURCES = List.of(
            new RetrievedChunk("片段 A", 0.9, "HR-001"),
            new RetrievedChunk("片段 B", 0.8, "FIN-001"),
            new RetrievedChunk("片段 C", 0.7, "IT-001"));

    @Test
    void recallAtKHitsWhenExpectedDocumentInTopK() {
        assertThat(RagEvaluator.recallAtK(SOURCES, "HR-001", 3)).isTrue();
        assertThat(RagEvaluator.recallAtK(SOURCES, "IT-001", 3)).isTrue();
    }

    @Test
    void recallAtKMissesWhenExpectedDocumentBeyondK() {
        assertThat(RagEvaluator.recallAtK(SOURCES, "IT-001", 2)).isFalse();
    }

    @Test
    void citationCorrectWhenCitedSourceMatches() {
        String answer = "根据 [1]，入职满一年享有 5 天年假。";
        assertThat(RagEvaluator.citationCorrect(answer, SOURCES, "HR-001")).isTrue();
    }

    @Test
    void citationIncorrectWhenCitedSourceDoesNotMatch() {
        String answer = "根据 [1] 和 [2] 的回答。";
        assertThat(RagEvaluator.citationCorrect(answer, SOURCES, "IT-001")).isFalse();
    }

    @Test
    void metricsComputesRates() {
        RagEvalMetrics metrics = RagEvaluator.metrics(4, 3, 2);
        assertThat(metrics.total()).isEqualTo(4);
        assertThat(metrics.recallRate()).isEqualTo(0.75);
        assertThat(metrics.citationAccuracy()).isEqualTo(0.5);
    }
}