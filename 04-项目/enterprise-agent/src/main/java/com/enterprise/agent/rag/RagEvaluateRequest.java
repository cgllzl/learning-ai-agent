package com.enterprise.agent.rag;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RagEvaluateRequest(
        @NotEmpty(message = "cases 不能为空") List<RagEvalCase> cases
) {
}