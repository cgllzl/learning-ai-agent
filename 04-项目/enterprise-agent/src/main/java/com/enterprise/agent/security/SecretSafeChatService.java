package com.enterprise.agent.security;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;
import java.util.List;

/**
 * 演示「密钥不落地、不打印」的安全聊天服务。
 * 密钥只保存在 SecretValue 里，对外展示永远用 masked()。
 */
public class SecretSafeChatService {

    private static final String BASE_URL = "https://api.deepseek.com";
    private static final String MODEL_NAME = "deepseek-chat";

    private final SecretValue apiKey;
    private final OpenAiChatModel chatModel;

    public SecretSafeChatService(String apiKey) {
        this.apiKey = SecretValue.of(apiKey);
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(apiKey)
                .modelName(MODEL_NAME)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    public String chat(String systemPrompt, String userMessage) {
        ChatResponse response = chatModel.chat(List.of(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userMessage)));
        return response.aiMessage().text();
    }

    public String safeConfigSummary() {
        return "api-key=" + apiKey.masked();
    }
}
