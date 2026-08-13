package com.enterprise.agent.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class StreamingChatServiceTest {

    private final OpenAiStreamingChatModel chatModel = mock(OpenAiStreamingChatModel.class);
    private final StreamingChatService service = new StreamingChatService(chatModel);

    @Test
    void streamsPartialResponsesUntilComplete() {
        doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onPartialResponse("你");
            handler.onPartialResponse("好");
            handler.onCompleteResponse(ChatResponse.builder().aiMessage(new AiMessage("你好")).build());
            return null;
        }).when(chatModel).chat(anyList(), org.mockito.ArgumentMatchers.any(StreamingChatResponseHandler.class));

        List<String> partials = new ArrayList<>();
        boolean[] completed = {false};
        service.stream(
                "你是助手",
                List.of(new ChatRequest.Message("user", "你好")),
                partials::add,
                () -> completed[0] = true,
                error -> {
                });

        assertThat(partials).containsExactly("你", "好");
        assertThat(completed[0]).isTrue();
    }

    @Test
    void forwardsErrorToCallback() {
        doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onError(new RuntimeException("boom"));
            return null;
        }).when(chatModel).chat(anyList(), org.mockito.ArgumentMatchers.any(StreamingChatResponseHandler.class));

        Throwable[] caught = {null};
        service.stream(
                null,
                List.of(new ChatRequest.Message("user", "hi")),
                partial -> {
                },
                () -> {
                },
                error -> caught[0] = error);

        assertThat(caught[0]).hasMessage("boom");
    }

    @Test
    void rejectsUnknownRole() {
        assertThatThrownBy(() -> service.stream(
                null,
                List.of(new ChatRequest.Message("robot", "hi")),
                partial -> {
                },
                () -> {
                },
                error -> {
                }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的 role");
    }
}