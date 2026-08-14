package com.enterprise.agent.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final OpenAiChatModel chatModel;

    public ChatService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String chat(String systemPrompt, List<ChatRequest.Message> messages) {
        List<ChatMessage> chatMessages = Messages.toLangChain4jMessages(systemPrompt, messages);
        ChatResponse response = chatModel.chat(chatMessages);
        AiMessage answer = response.aiMessage();
        if (answer == null || answer.text() == null || answer.text().isBlank()) {
            throw new IllegalStateException("模型未返回内容");
        }
        return answer.text();
    }
}