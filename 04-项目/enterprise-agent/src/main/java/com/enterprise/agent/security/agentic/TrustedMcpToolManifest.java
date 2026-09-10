package com.enterprise.agent.security.agentic;

import java.util.Set;

/**
 * 审核通过后固定下来的 MCP Tool 清单。
 */
public record TrustedMcpToolManifest(
        String serverId,
        String toolName,
        String version,
        String descriptionFingerprint,
        Set<String> allowedHosts
) {

    public TrustedMcpToolManifest {
        allowedHosts = Set.copyOf(allowedHosts);
    }
}
