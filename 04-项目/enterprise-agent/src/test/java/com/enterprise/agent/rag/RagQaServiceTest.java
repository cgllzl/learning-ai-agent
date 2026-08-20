package com.enterprise.agent.rag;

import com.enterprise.agent.chat.ResilientCaller;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagQaServiceTest {

    @Test
    void returnsAnswerWithSources() {
        HybridSearchService hybridSearchService = mock(HybridSearchService.class);
        when(hybridSearchService.search(any(), any(), any(), any())).thenReturn(List.of(
                new RetrievedChunk("虚拟线程提升并发吞吐量", 0.95, "DOC1")));

        ResilientCaller resilientCaller = mock(ResilientCaller.class);
        when(resilientCaller.callWithFallback(any())).thenReturn(
                ChatResponse.builder().aiMessage(new AiMessage("根据 [1]，虚拟线程可以提升并发吞吐量。")).build());

        RagQaService ragQaService = new RagQaService(hybridSearchService, resilientCaller);
        RagChatResponse response = ragQaService.ask("虚拟线程有什么用？", null, 5);

        assertThat(response.answer()).contains("虚拟线程");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).documentId()).isEqualTo("DOC1");
    }
}