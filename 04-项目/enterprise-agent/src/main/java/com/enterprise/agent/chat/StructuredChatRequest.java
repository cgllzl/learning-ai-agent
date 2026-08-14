package com.enterprise.agent.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StructuredChatRequest(
        String systemPrompt,
        @Valid @NotEmpty(message = "messages 不能为空") List<ChatRequest.Message> messages,
        String schema,
        String mode
) {
}