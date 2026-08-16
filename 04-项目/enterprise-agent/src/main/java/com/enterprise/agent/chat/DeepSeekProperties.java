package com.enterprise.agent.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "deepseek")
public record DeepSeekProperties(
        String apiKey,
        String baseUrl,
        String model,
        Duration timeout,
        Integer maxRetries,
        String fallbackModel
) {
    public DeepSeekProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.deepseek.com";
        }
        if (model == null || model.isBlank()) {
            model = "deepseek-chat";
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(30);
        }
        if (maxRetries == null) {
            maxRetries = 2;
        }
        if (fallbackModel == null || fallbackModel.isBlank()) {
            fallbackModel = model;
        }
    }
}