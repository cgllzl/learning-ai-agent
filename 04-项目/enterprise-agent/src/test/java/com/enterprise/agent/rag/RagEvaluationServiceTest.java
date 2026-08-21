package com.enterprise.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagEvaluationServiceTest {

    @Test
    void computesRecallAndCitationMetrics() {
        RagQaService ragQaService = mock(RagQaService.class);
        when(ragQaService.ask(any(), any(), any())).thenAnswer(invocation -> {
            String question = invocation.getArgument(0);
            if (question.contains("年假")) {
                return new RagChatResponse("根据 [1] 的回答。",
                        List.of(new RetrievedChunk("年假 5 天", 0.9, "HR-001")));
            }
            return new RagChatResponse("根据 [1] 的回答。",
                    List.of(new RetrievedChunk("报销超 500 需审批", 0.9, "FIN-001")));
        });

        RagEvaluationService service = new RagEvaluationService(ragQaService);
        RagEvalMetrics metrics = service.evaluate(List.of(
                new RagEvalCase("年假有几天？", "HR-001"),
                new RagEvalCase("报销怎么报？", "FIN-001")));

        assertThat(metrics.total()).isEqualTo(2);
        assertThat(metrics.recallRate()).isEqualTo(1.0);
        assertThat(metrics.citationAccuracy()).isEqualTo(1.0);
    }
}