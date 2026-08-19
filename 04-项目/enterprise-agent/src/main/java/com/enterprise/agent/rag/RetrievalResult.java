package com.enterprise.agent.rag;

import java.util.List;

public record RetrievalResult(List<RetrievedChunk> chunks) {
}