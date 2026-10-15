package com.enterprise.agent.mcp;

import com.enterprise.agent.security.SecretValue;
import com.enterprise.agent.security.agentic.AgenticSecurityBlockedException;
import com.enterprise.agent.security.mcp.McpRemoteCredential;
import com.enterprise.agent.security.mcp.McpServerRegistration;
import com.enterprise.agent.security.mcp.SecureMcpRemoteTransportFactory;
import dev.langchain4j.mcp.client.McpHeadersSupplier;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Day 5 远程凭证边界测试。
 * 这是远程 OAuth 的离线配置验证，不代表已经完成真实授权服务器联调。
 */
class McpRemoteCredentialTest {

    private static final URI MCP_ENDPOINT = URI.create("https://crm.partner.example/mcp");

    @Test
    @DisplayName("目的：只有 audience 精确匹配且 Scope 足够时才生成 Authorization Header")
    void credentialRequiresExactAudienceAndSufficientScope() {
        McpRemoteCredential credential = credential();

        assertThat(credential.authorizationHeadersFor(
                MCP_ENDPOINT, Set.of("crm:read")))
                .containsEntry("Authorization", "Bearer live-secret-token");
        assertThatThrownBy(() -> credential.authorizationHeadersFor(
                URI.create("https://downstream.partner.example/api"), Set.of("crm:read")))
                .isInstanceOf(AgenticSecurityBlockedException.class)
                .hasMessageContaining("audience");
        assertThatThrownBy(() -> credential.authorizationHeadersFor(
                MCP_ENDPOINT, Set.of("crm:write")))
                .isInstanceOf(AgenticSecurityBlockedException.class)
                .hasMessageContaining("Scope");
    }

    @Test
    @DisplayName("目的：验证 Credential 脱敏，以及 Transport 关闭日志、禁重定向并绑定登记主机")
    void transportUsesReviewedEndpointAndSafeDefaults() {
        McpRemoteCredential credential = credential();
        StreamableHttpMcpTransport transport = SecureMcpRemoteTransportFactory.create(
                registration(), credential, Set.of("crm:read"), Duration.ofSeconds(10));

        assertThat(credential.toString()).doesNotContain("live-secret-token");
        assertThat(ReflectionTestUtils.getField(transport, "logRequests")).isEqualTo(false);
        assertThat(ReflectionTestUtils.getField(transport, "logResponses")).isEqualTo(false);

        HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(transport, "httpClient");
        assertThat(httpClient).isNotNull();
        assertThat(httpClient.followRedirects()).isEqualTo(HttpClient.Redirect.NEVER);
        assertThat(httpClient.connectTimeout()).contains(Duration.ofSeconds(10));

        McpHeadersSupplier supplier = (McpHeadersSupplier)
                ReflectionTestUtils.getField(transport, "customHeadersSupplier");
        Map<String, String> headers = supplier.apply(null);
        assertThat(headers).containsEntry("Authorization", "Bearer live-secret-token");

        McpServerRegistration mismatched = new McpServerRegistration(
                "crm-remote",
                "reviewed-crm-server",
                "1.0.0",
                "sha256:" + "e".repeat(64),
                "evil.example",
                MCP_ENDPOINT);

        assertThatThrownBy(() -> SecureMcpRemoteTransportFactory.create(
                mismatched, credential(), Set.of("crm:read"), Duration.ofSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetHost");
    }

    private McpRemoteCredential credential() {
        return new McpRemoteCredential(
                MCP_ENDPOINT,
                Set.of("crm:read"),
                SecretValue.of("live-secret-token"));
    }

    private McpServerRegistration registration() {
        return new McpServerRegistration(
                "crm-remote",
                "reviewed-crm-server",
                "1.0.0",
                "sha256:" + "d".repeat(64),
                "crm.partner.example",
                MCP_ENDPOINT);
    }
}
