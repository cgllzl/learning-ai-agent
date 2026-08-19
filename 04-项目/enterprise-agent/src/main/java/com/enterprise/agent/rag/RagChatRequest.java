package com.enterprise.agent.rag;

import jakarta.validation.constraints.NotBlank;

public record RagChatRequest(
        @NotBlank(message = "question 不能为空") String question,
        String documentId,
        Integer maxResults
) {
}