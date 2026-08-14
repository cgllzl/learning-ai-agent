package com.enterprise.agent.chat;

import com.fasterxml.jackson.databind.JsonNode;

public record StructuredChatResponse(JsonNode result) {
}