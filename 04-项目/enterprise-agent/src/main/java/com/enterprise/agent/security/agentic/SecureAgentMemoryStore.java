package com.enterprise.agent.security.agentic;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Memory 写入前扫描，并按租户、用户、会话隔离；可疑内容进入隔离区，不参与后续上下文。
 */
@Component
public class SecureAgentMemoryStore {

    private final AgenticContentGuard contentGuard;
    private final AuditLogService auditLog;
    private final List<SecureMemoryEntry> entries = new CopyOnWriteArrayList<>();

    public SecureAgentMemoryStore(AgenticContentGuard contentGuard, AuditLogService auditLog) {
        this.contentGuard = contentGuard;
        this.auditLog = auditLog;
    }

    public SecureMemoryEntry write(SecuritySubject subject,
                                   String sessionId,
                                   UntrustedContentSource source,
                                   String text,
                                   Duration ttl) {
        String entryId = UUID.randomUUID().toString();
        ContentInspection inspection = contentGuard.inspect(
                new UntrustedContent(source, entryId, text));
        SecureMemoryEntry entry = new SecureMemoryEntry(
                entryId,
                subject.tenantId(),
                subject.userId(),
                sessionId,
                source,
                text,
                Instant.now().plus(ttl),
                !inspection.safe()
        );
        entries.add(entry);
        auditLog.record(subject, "memoryWrite",
                "entryId=" + entryId + ", source=" + source,
                inspection.safe() ? "ACTIVE" : "QUARANTINED: " + inspection.reason(),
                inspection.safe() ? AuditStatus.SUCCESS : AuditStatus.BLOCKED);
        return entry;
    }

    public List<SecureMemoryEntry> activeEntries(SecuritySubject subject, String sessionId) {
        Instant now = Instant.now();
        return entries.stream()
                .filter(entry -> entry.tenantId().equals(subject.tenantId()))
                .filter(entry -> entry.userId().equals(subject.userId()))
                .filter(entry -> entry.sessionId().equals(sessionId))
                .filter(entry -> !entry.quarantined())
                .filter(entry -> now.isBefore(entry.expiresAt()))
                .toList();
    }
}
