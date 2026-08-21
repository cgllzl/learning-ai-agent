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
    void citationCorrectWhenAllCitationsPointToExpectedDocument() {
        List<RetrievedChunk> multiSame = List.of(
                new RetrievedChunk("片段 A", 0.9, "HR-001"),
                new RetrievedChunk("片段 B", 0.8, "HR-001"));
        assertThat(RagEvaluator.citationCorrect("根据 [1] 和 [2] 的回答。", multiSame, "HR-001")).isTrue();
    }

    @Test
    void citationIncorrectWhenAnyCitationIsWrong() {
        // [1] 是 HR-001、[2] 是 FIN-001；期望 HR-001，但 [2] 错了 → 严格模式判错
        assertThat(RagEvaluator.citationCorrect("根据 [1] 和 [2] 的回答。", SOURCES, "HR-001")).isFalse();
    }

    @Test
    void citationIncorrectWhenNoCitationOrOutOfRange() {
        assertThat(RagEvaluator.citationCorrect("没有任何引用。", SOURCES, "HR-001")).isFalse();
        assertThat(RagEvaluator.citationCorrect("越界引用 [9]。", SOURCES, "HR-001")).isFalse();
    }

    @Test
    void citationPrecisionComputesRatio() {
        List<RetrievedChunk> sources = List.of(
                new RetrievedChunk("A", 0.9, "FIN-001"),
                new RetrievedChunk("B", 0.8, "FIN-001"),
                new RetrievedChunk("C", 0.7, "IT-001"));
        // [1]、[2] 命中 FIN-001，[3] 是 IT-001 → 2/3
        assertThat(RagEvaluator.citationPrecision("参考 [1] [2] [3]", sources, "FIN-001")).isEqualTo(2.0 / 3.0);
    }

    @Test
    void citationPrecisionIsZeroWithoutCitations() {
        assertThat(RagEvaluator.citationPrecision("没有引用", SOURCES, "HR-001")).isEqualTo(0.0);
    }

    @Test
    void metricsComputesRates() {
        RagEvalMetrics metrics = RagEvaluator.metrics(4, 3, 2, 0.5);
        assertThat(metrics.total()).isEqualTo(4);
        assertThat(metrics.recallRate()).isEqualTo(0.75);
        assertThat(metrics.citationAccuracy()).isEqualTo(0.5);
        assertThat(metrics.citationPrecision()).isEqualTo(0.5);
    }
}