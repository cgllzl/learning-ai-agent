package com.enterprise.agent.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private final OpenAiChatModel chatModel;

    public ChatService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String chat(String systemPrompt, List<ChatRequest.Message> messages) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            chatMessages.add(SystemMessage.from(systemPrompt));
        }
        for (ChatRequest.Message message : messages) {
            chatMessages.add(toLangChain4jMessage(message));
        }

        ChatResponse response = chatModel.chat(chatMessages);
        AiMessage answer = response.aiMessage();
        if (answer == null || answer.text() == null || answer.text().isBlank()) {
            throw new IllegalStateException("模型未返回内容");
        }
        return answer.text();
    }

    private ChatMessage toLangChain4jMessage(ChatRequest.Message message) {
        return switch (message.role().toLowerCase()) {
            case "system" -> SystemMessage.from(message.content());
            case "user" -> UserMessage.from(message.content());
            case "assistant" -> new AiMessage(message.content());
            default -> throw new IllegalArgumentException("不支持的 role: " + message.role());
        };
    }
}