package com.enterprise.agent.rag;

public record RetrievedChunk(String text, double score, String documentId) {
}