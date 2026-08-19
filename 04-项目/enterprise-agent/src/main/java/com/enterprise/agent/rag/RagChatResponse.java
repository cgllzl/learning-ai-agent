package com.enterprise.agent.rag;

import java.util.List;

public record RagChatResponse(String answer, List<RetrievedChunk> sources) {
}