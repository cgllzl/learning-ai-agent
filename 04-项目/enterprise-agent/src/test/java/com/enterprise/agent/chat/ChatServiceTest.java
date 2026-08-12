package com.enterprise.agent.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private final OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
    private final ChatService chatService = new ChatService(chatModel);

    @Test
    void returnsModelReply() {
        when(chatModel.chat(anyList())).thenReturn(
                ChatResponse.builder().aiMessage(new AiMessage("你好，我是企业助手")).build());

        String reply = chatService.chat(
                "你是企业助手，回答要简洁。",
                List.of(new ChatRequest.Message("user", "你好"))
        );

        assertThat(reply).isEqualTo("你好，我是企业助手");
    }

    @Test
    void omitsBlankSystemPrompt() {
        when(chatModel.chat(anyList())).thenReturn(
                ChatResponse.builder().aiMessage(new AiMessage("ok")).build());

        String reply = chatService.chat(
                "   ",
                List.of(new ChatRequest.Message("user", "hi"))
        );

        assertThat(reply).isEqualTo("ok");
    }

    @Test
    void rejectsUnknownRole() {
        assertThatThrownBy(() ->
                chatService.chat(null, List.of(new ChatRequest.Message("robot", "hi"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的 role");
    }
}