package com.enterprise.agent.security.agentic;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 像核对商品防伪码一样，核对 MCP 来源、名称、版本、描述指纹和出站主机。
 */
@Component
public class McpToolTrustVerifier {

    private final AuditLogService auditLog;

    public McpToolTrustVerifier(AuditLogService auditLog) {
        this.auditLog = auditLog;
    }

    public void verify(SecuritySubject subject,
                       McpToolDescriptor actual,
                       TrustedMcpToolManifest trusted) {
        boolean identityMatches = trusted.serverId().equals(actual.serverId())
                && trusted.toolName().equals(actual.toolName())
                && trusted.version().equals(actual.version());
        boolean descriptionMatches = trusted.descriptionFingerprint()
                .equals(fingerprint(actual.description()));
        boolean hostAllowed = trusted.allowedHosts().contains(actual.targetHost());

        if (!identityMatches || !descriptionMatches || !hostAllowed) {
            String reason = "MCP Tool 身份、版本、描述或出站主机与审核清单不一致";
            auditLog.record(subject, "mcpTrust:" + actual.toolName(),
                    safeIdentity(actual), reason, AuditStatus.BLOCKED);
            throw new AgenticSecurityBlockedException(
                    reason, AgenticRisk.TOOL_DESCRIPTOR_TAMPERING);
        }

        auditLog.record(subject, "mcpTrust:" + actual.toolName(),
                safeIdentity(actual), "TRUSTED", AuditStatus.SUCCESS);
    }

    public static String fingerprint(String description) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(description.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }

    private String safeIdentity(McpToolDescriptor descriptor) {
        return descriptor.serverId() + "/" + descriptor.toolName()
                + "@" + descriptor.version() + " -> " + descriptor.targetHost();
    }
}
