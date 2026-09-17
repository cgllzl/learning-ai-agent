package com.enterprise.agent.security.mcp;

import java.net.URI;
import java.util.Objects;

/**
 * 由企业管理员审核的 MCP 部署记录，而不是直接相信 Server 自报的 serverInfo。
 *
 * <p>本记录本身不会计算运行中进程或镜像的摘要；部署流水线必须先验证制品，
 * 再把对应的 {@code McpClient} 实例与这条记录绑定。resourceAudience 用来约束 OAuth Token 的目标。</p>
 */
public record McpServerRegistration(
        String clientKey,
        String serverId,
        String serverVersion,
        String artifactDigest,
        String targetHost,
        URI resourceAudience
) {

    public McpServerRegistration {
        Objects.requireNonNull(clientKey, "clientKey");
        Objects.requireNonNull(serverId, "serverId");
        Objects.requireNonNull(serverVersion, "serverVersion");
        Objects.requireNonNull(artifactDigest, "artifactDigest");
        Objects.requireNonNull(targetHost, "targetHost");
        Objects.requireNonNull(resourceAudience, "resourceAudience");
        if (!artifactDigest.matches("sha256:[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("MCP Server 必须固定 64 位十六进制 sha256 制品摘要");
        }
    }
}
