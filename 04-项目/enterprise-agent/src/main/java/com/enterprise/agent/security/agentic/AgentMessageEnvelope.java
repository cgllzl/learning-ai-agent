package com.enterprise.agent.security.agentic;

import java.time.Instant;
import java.util.Set;

/**
 * 跨 Agent 传递的是带身份、受众、Scope、Trace 和有效期的结构化信封，而不是裸字符串。
 */
public record AgentMessageEnvelope(
        String messageId,
        String traceId,
        String originalUserId,
        String tenantId,
        String senderAgent,
        String recipientAgent,
        Set<String> delegatedScopes,
        String payload,
        Instant issuedAt,
        Instant expiresAt,
        String signature
) {

    public AgentMessageEnvelope {
        delegatedScopes = Set.copyOf(delegatedScopes);
    }

    public AgentMessageEnvelope withSignature(String newSignature) {
        return new AgentMessageEnvelope(
                messageId, traceId, originalUserId, tenantId,
                senderAgent, recipientAgent, delegatedScopes, payload,
                issuedAt, expiresAt, newSignature);
    }
}
