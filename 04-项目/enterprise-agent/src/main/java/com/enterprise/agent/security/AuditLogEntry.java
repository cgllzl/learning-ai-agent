package com.enterprise.agent.security;

import java.time.Instant;

/**
 * 一条审计日志：谁、在哪个租户、调用了哪个工具、结果如何。
 */
public record AuditLogEntry(
        Instant timestamp,
        String userId,
        String tenantId,
        String toolName,
        String arguments,
        String result,
        AuditStatus status) {
}
