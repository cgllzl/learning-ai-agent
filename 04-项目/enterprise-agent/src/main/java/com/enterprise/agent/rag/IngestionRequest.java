package com.enterprise.agent.rag;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record IngestionRequest(
        @NotBlank(message = "documentId 不能为空") String documentId,
        @NotBlank(message = "content 不能为空") String content,
        Map<String, Object> metadata
) {
}