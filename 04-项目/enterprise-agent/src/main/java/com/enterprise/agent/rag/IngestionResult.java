package com.enterprise.agent.rag;

import java.util.List;

public record IngestionResult(String documentId, int segmentCount, List<String> segmentIds) {
}