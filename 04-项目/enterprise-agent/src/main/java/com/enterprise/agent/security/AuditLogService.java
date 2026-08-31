package com.enterprise.agent.security;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存版审计日志：把每次关键动作记下来。
 * 生产环境通常写数据库或日志系统，这里用内存列表便于学习和测试。
 */
public class AuditLogService {

    private final List<AuditLogEntry> entries = new CopyOnWriteArrayList<>();

    public void record(SecuritySubject subject, String toolName, String arguments,
                       String result, AuditStatus status) {
        entries.add(new AuditLogEntry(
                Instant.now(),
                subject.userId(),
                subject.tenantId(),
                toolName,
                arguments,
                result,
                status));
    }

    public List<AuditLogEntry> entries() {
        return List.copyOf(entries);
    }
}
