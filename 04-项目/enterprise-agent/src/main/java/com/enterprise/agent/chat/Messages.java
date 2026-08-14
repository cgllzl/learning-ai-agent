package com.enterprise.agent.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.ArrayList;
import java.util.List;

final class Messages {

    private Messages() {
    }

    static List<ChatMessage> toLangChain4jMessages(String systemPrompt, List<ChatRequest.Message> messages) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            chatMessages.add(SystemMessage.from(systemPrompt));
        }
        for (ChatRequest.Message message : messages) {
            chatMessages.add(toLangChain4jMessage(message));
        }
        return chatMessages;
    }

    private static ChatMessage toLangChain4jMessage(ChatRequest.Message message) {
        return switch (message.role().toLowerCase()) {
            case "system" -> SystemMessage.from(message.content());
            case "user" -> UserMessage.from(message.content());
            case "assistant" -> new AiMessage(message.content());
            default -> throw new IllegalArgumentException("不支持的 role: " + message.role());
        };
    }
}