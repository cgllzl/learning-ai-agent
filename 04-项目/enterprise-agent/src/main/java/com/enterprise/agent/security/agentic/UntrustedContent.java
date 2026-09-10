package com.enterprise.agent.security.agentic;

public record UntrustedContent(
        UntrustedContentSource source,
        String sourceId,
        String text
) {
}
