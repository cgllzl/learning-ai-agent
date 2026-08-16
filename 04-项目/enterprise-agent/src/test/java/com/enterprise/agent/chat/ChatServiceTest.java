package com.enterprise.agent.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private final ResilientCaller resilientCaller = mock(ResilientCaller.class);
    private final ChatService chatService = new ChatService(resilientCaller);

    @BeforeEach
    void setUp() {
        when(resilientCaller.callWithFallback(any())).thenReturn(
                ChatResponse.builder().aiMessage(new AiMessage("你好，我是企业助手")).build());
    }

    @Test
    void returnsModelReply() {
        String reply = chatService.chat(
                "你是企业助手，回答要简洁。",
                List.of(new ChatRequest.Message("user", "你好"))
        );

        assertThat(reply).isEqualTo("你好，我是企业助手");
    }

    @Test
    void omitsBlankSystemPrompt() {
        String reply = chatService.chat(
                "   ",
                List.of(new ChatRequest.Message("user", "hi"))
        );

        assertThat(reply).isEqualTo("你好，我是企业助手");
    }

    @Test
    void rejectsUnknownRole() {
        assertThatThrownBy(() ->
                chatService.chat(null, List.of(new ChatRequest.Message("robot", "hi"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的 role");
    }
}