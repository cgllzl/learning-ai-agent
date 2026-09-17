package com.enterprise.agent.security.mcp;

import com.enterprise.agent.security.SecretValue;
import com.enterprise.agent.security.agentic.AgenticRisk;
import com.enterprise.agent.security.agentic.AgenticSecurityBlockedException;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * 已由外部 OAuth 流程取得的远程 MCP 凭证。
 *
 * <p>本类不伪装成完整 OAuth Client；它只守住三个执行边界：Token audience 必须精确匹配、
 * Scope 必须足够、原始 Token 不进入 {@code toString()}。MCP Server 调下游 API 时还应使用另一枚 Token，
 * 不能把这里的 Token 继续透传。audience/scopes 必须来自已经验证过的 Token 或可信授权组件，
 * 不能直接相信用户输入。</p>
 */
public final class McpRemoteCredential {

    private final URI audience;
    private final Set<String> scopes;
    private final SecretValue accessToken;

    public McpRemoteCredential(URI audience, Set<String> scopes, SecretValue accessToken) {
        if (!"https".equalsIgnoreCase(audience.getScheme())) {
            throw new IllegalArgumentException("远程 MCP audience 必须使用 HTTPS");
        }
        this.audience = audience.normalize();
        this.scopes = Set.copyOf(scopes);
        this.accessToken = accessToken;
    }

    public Map<String, String> authorizationHeadersFor(URI target,
                                                        Set<String> requiredScopes) {
        if (!audience.equals(target.normalize())) {
            throw new AgenticSecurityBlockedException(
                    "Token audience 与目标 MCP 不一致，禁止发送凭证",
                    AgenticRisk.IDENTITY_MISMATCH);
        }
        if (!scopes.containsAll(requiredScopes)) {
            throw new AgenticSecurityBlockedException(
                    "Token 缺少 MCP Tool 所需 Scope",
                    AgenticRisk.SCOPE_VIOLATION);
        }
        return Map.of("Authorization", "Bearer " + accessToken.raw());
    }

    @Override
    public String toString() {
        return "McpRemoteCredential[audience=" + audience + ", scopes=" + scopes + ", token=***]";
    }
}
