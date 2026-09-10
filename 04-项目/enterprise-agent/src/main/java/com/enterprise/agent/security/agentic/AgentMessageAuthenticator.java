package com.enterprise.agent.security.agentic;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 学习版 Agent 消息认证：HMAC 防篡改，messageId 防重放，受众/租户/Scope 防越权。
 */
public class AgentMessageAuthenticator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] sharedSecret;
    private final AuditLogService auditLog;
    private final Set<String> consumedMessageIds = ConcurrentHashMap.newKeySet();

    public AgentMessageAuthenticator(String sharedSecret, AuditLogService auditLog) {
        this.sharedSecret = sharedSecret.getBytes(StandardCharsets.UTF_8);
        this.auditLog = auditLog;
    }

    public AgentMessageEnvelope sign(AgentMessageEnvelope envelope) {
        return envelope.withSignature(calculateSignature(envelope));
    }

    public void verifyAndConsume(AgentMessageEnvelope envelope,
                                 SecuritySubject currentSubject,
                                 String expectedRecipient,
                                 Set<String> permittedScopes) {
        String expectedSignature = calculateSignature(envelope);
        if (envelope.signature() == null || !MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                envelope.signature().getBytes(StandardCharsets.UTF_8))) {
            block(currentSubject, envelope, AgenticRisk.MESSAGE_TAMPERING,
                    "Agent 消息签名无效，内容可能被篡改");
        }
        if (Instant.now().isAfter(envelope.expiresAt())) {
            block(currentSubject, envelope, AgenticRisk.EXPIRED_MESSAGE,
                    "Agent 消息已过期");
        }
        if (!currentSubject.userId().equals(envelope.originalUserId())
                || !currentSubject.tenantId().equals(envelope.tenantId())
                || !expectedRecipient.equals(envelope.recipientAgent())) {
            block(currentSubject, envelope, AgenticRisk.IDENTITY_MISMATCH,
                    "Agent 消息的原始用户、租户或接收者不匹配");
        }
        if (!permittedScopes.containsAll(envelope.delegatedScopes())) {
            block(currentSubject, envelope, AgenticRisk.SCOPE_VIOLATION,
                    "子 Agent 试图扩大委托 Scope");
        }
        if (!consumedMessageIds.add(envelope.messageId())) {
            block(currentSubject, envelope, AgenticRisk.REPLAYED_MESSAGE,
                    "相同 Agent 消息不能重复消费");
        }

        auditLog.record(currentSubject, "agentMessage:" + envelope.senderAgent(),
                safeIdentity(envelope), "VERIFIED", AuditStatus.SUCCESS);
    }

    private String calculateSignature(AgentMessageEnvelope envelope) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(sharedSecret, HMAC_ALGORITHM));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(canonical(envelope).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法计算 Agent 消息签名", e);
        }
    }

    private String canonical(AgentMessageEnvelope envelope) {
        String scopes = envelope.delegatedScopes().stream()
                .sorted()
                .collect(Collectors.joining(","));
        return String.join("|",
                envelope.messageId(),
                envelope.traceId(),
                envelope.originalUserId(),
                envelope.tenantId(),
                envelope.senderAgent(),
                envelope.recipientAgent(),
                scopes,
                envelope.payload(),
                envelope.issuedAt().toString(),
                envelope.expiresAt().toString());
    }

    private void block(SecuritySubject subject,
                       AgentMessageEnvelope envelope,
                       AgenticRisk risk,
                       String reason) {
        auditLog.record(subject, "agentMessage:" + envelope.senderAgent(),
                safeIdentity(envelope), reason, AuditStatus.BLOCKED);
        throw new AgenticSecurityBlockedException(reason, risk);
    }

    private String safeIdentity(AgentMessageEnvelope envelope) {
        return "messageId=" + envelope.messageId()
                + ", traceId=" + envelope.traceId()
                + ", sender=" + envelope.senderAgent()
                + ", recipient=" + envelope.recipientAgent()
                + ", scopes=" + envelope.delegatedScopes();
    }
}
