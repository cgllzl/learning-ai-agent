package com.enterprise.agent.rag;

import jakarta.validation.constraints.NotBlank;

public record RetrievalRequest(
        @NotBlank(message = "query 不能为空") String query,
        String documentId,
        Integer maxResults,
        Double minScore
) {
}