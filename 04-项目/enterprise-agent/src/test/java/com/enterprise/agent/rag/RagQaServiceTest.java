package com.enterprise.agent.rag;

import com.enterprise.agent.chat.ResilientCaller;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagQaServiceTest {

    @Test
    void returnsAnswerWithSources() {
        DocumentRetrievalService retrievalService = mock(DocumentRetrievalService.class);
        when(retrievalService.retrieve(any(), any(), any(), any())).thenReturn(List.of(
                new RetrievedChunk("虚拟线程提升并发吞吐量", 0.95, "DOC1")));

        ResilientCaller resilientCaller = mock(ResilientCaller.class);
        when(resilientCaller.callWithFallback(any())).thenReturn(
                ChatResponse.builder().aiMessage(new AiMessage("根据 [1]，虚拟线程可以提升并发吞吐量。")).build());

        RagQaService ragQaService = new RagQaService(retrievalService, resilientCaller);
        RagChatResponse response = ragQaService.ask("虚拟线程有什么用？", null, 5);

        assertThat(response.answer()).contains("虚拟线程");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).documentId()).isEqualTo("DOC1");
    }

    @Test
    void includesRetrievedContextInPrompt() {
        DocumentRetrievalService retrievalService = mock(DocumentRetrievalService.class);
        when(retrievalService.retrieve(any(), any(), any(), any())).thenReturn(List.of(
                new RetrievedChunk("企业假期待遇说明", 0.9, "HR-001")));

        ResilientCaller resilientCaller = mock(ResilientCaller.class);
        // 捕获传给模型的 prompt，验证参考资料确实被拼接
        when(resilientCaller.callWithFallback(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Function<dev.langchain4j.model.openai.OpenAiChatModel, ChatResponse> fn =
                    invocation.getArgument(0);
            dev.langchain4j.model.openai.OpenAiChatModel model = mock(dev.langchain4j.model.openai.OpenAiChatModel.class);
            when(model.chat(anyList())).thenReturn(
                    ChatResponse.builder().aiMessage(new AiMessage("根据 [1] 回答")).build());
            return fn.apply(model);
        });

        RagQaService ragQaService = new RagQaService(retrievalService, resilientCaller);
        RagChatResponse response = ragQaService.ask("公司年假几天？", null, 5);

        assertThat(response.answer()).contains("[1]");
    }
}