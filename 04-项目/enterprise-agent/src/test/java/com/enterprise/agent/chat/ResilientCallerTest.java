package com.enterprise.agent.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.InternalServerException;
import dev.langchain4j.exception.InvalidRequestException;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResilientCallerTest {

    private static final List<ChatMessage> MESSAGES = List.of(UserMessage.from("hi"));

    private final DeepSeekProperties props =
            new DeepSeekProperties("key", "https://api.deepseek.com", "primary-model",
                    Duration.ofSeconds(30), 2, "fallback-model");
    private final OpenAiChatModel primary = mock(OpenAiChatModel.class);
    private final OpenAiChatModel fallback = mock(OpenAiChatModel.class);
    private final ResilientCaller caller = new ResilientCaller(props, primary, fallback);

    private ChatResponse okResponse() {
        return ChatResponse.builder().aiMessage(new AiMessage("ok")).build();
    }

    @Test
    void succeedsImmediatelyOnFirstAttempt() {
        when(primary.chat(MESSAGES)).thenReturn(okResponse());

        ChatResponse response = caller.callWithFallback(model -> model.chat(MESSAGES));

        assertThat(response.aiMessage().text()).isEqualTo("ok");
        verify(primary, times(1)).chat(MESSAGES);
        verify(fallback, never()).chat(anyList());
    }

    @Test
    void retriesTransientFailureThenSucceeds() {
        when(primary.chat(MESSAGES))
                .thenThrow(new InternalServerException("boom"))
                .thenThrow(new InternalServerException("boom"))
                .thenReturn(okResponse());

        ChatResponse response = caller.callWithFallback(model -> model.chat(MESSAGES));

        assertThat(response).isNotNull();
        verify(primary, times(3)).chat(MESSAGES);
        verify(fallback, never()).chat(anyList());
    }

    @Test
    void failsFastOnNonRetriableError() {
        when(primary.chat(MESSAGES)).thenThrow(new InvalidRequestException("bad request"));

        assertThatThrownBy(() -> caller.callWithFallback(model -> model.chat(MESSAGES)))
                .isInstanceOf(InvalidRequestException.class);

        verify(primary, times(1)).chat(MESSAGES);
        verify(fallback, never()).chat(anyList());
    }

    @Test
    void fallsBackToSecondaryModelWhenPrimaryExhaustsRetries() {
        when(primary.chat(MESSAGES)).thenThrow(new InternalServerException("down"));
        when(fallback.chat(MESSAGES)).thenReturn(okResponse());

        ChatResponse response = caller.callWithFallback(model -> model.chat(MESSAGES));

        assertThat(response).isNotNull();
        verify(fallback, times(1)).chat(MESSAGES);
    }

    @Test
    void throwsAiServiceUnavailableWhenBothModelsFail() {
        when(primary.chat(MESSAGES)).thenThrow(new InternalServerException("down"));
        when(fallback.chat(MESSAGES)).thenThrow(new InternalServerException("down too"));

        assertThatThrownBy(() -> caller.callWithFallback(model -> model.chat(MESSAGES)))
                .isInstanceOf(AiServiceUnavailableException.class);
    }
}