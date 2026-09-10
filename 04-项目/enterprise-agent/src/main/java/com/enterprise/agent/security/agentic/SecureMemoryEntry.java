package com.enterprise.agent.security.agentic;

import java.time.Instant;

public record SecureMemoryEntry(
        String entryId,
        String tenantId,
        String userId,
        String sessionId,
        UntrustedContentSource source,
        String text,
        Instant expiresAt,
        boolean quarantined
) {
}
