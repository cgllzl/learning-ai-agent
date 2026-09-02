package com.enterprise.agent.observability;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.List;

/**
 * 记录 Token 用量、成本与延迟的聊天服务（Week 6 Day 4）。
 */
public class UsageAwareChatService {

    private final OpenAiChatModel chatModel;
    private final UsageMetricsService metricsService;

    public UsageAwareChatService(OpenAiChatModel chatModel, UsageMetricsService metricsService) {
        this.chatModel = chatModel;
        this.metricsService = metricsService;
    }

    public String chat(String systemPrompt, String userMessage) {
        long startNanos = System.nanoTime();
        ChatResponse response = chatModel.chat(List.of(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userMessage)));
        long durationMillis = (System.nanoTime() - startNanos) / 1_000_000;

        metricsService.record(durationMillis, response.tokenUsage());
        return response.aiMessage().text();
    }
}
