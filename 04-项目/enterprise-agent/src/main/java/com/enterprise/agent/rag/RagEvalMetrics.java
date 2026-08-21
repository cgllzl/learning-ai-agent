package com.enterprise.agent.rag;

public record RagEvalMetrics(
        int total,
        int recallHits,
        int citationHits,
        double recallRate,
        double citationAccuracy
) {
}