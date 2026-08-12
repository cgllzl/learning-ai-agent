package com.enterprise.agent.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ChatRequest(
        String systemPrompt,
        @Valid @NotEmpty(message = "messages 不能为空") List<Message> messages
) {
    public record Message(
            @NotBlank(message = "role 不能为空") String role,
            @NotBlank(message = "content 不能为空") String content
    ) {
    }
}