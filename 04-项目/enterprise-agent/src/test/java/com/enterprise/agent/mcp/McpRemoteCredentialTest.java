package com.enterprise.agent.mcp;

import com.enterprise.agent.security.SecretValue;
import com.enterprise.agent.security.agentic.AgenticSecurityBlockedException;
import com.enterprise.agent.security.mcp.McpRemoteCredential;
import com.enterprise.agent.security.mcp.McpServerRegistration;
import com.enterprise.agent.security.mcp.SecureMcpRemoteTransportFactory;
import dev.langchain4j.mcp.client.McpHeadersSupplier;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 验证远程 MCP Token 只能发送给正确 audience，且不会出现在对象日志文本中。 */
class McpRemoteCredentialTest {

    private static final URI MCP_ENDPOINT = URI.create("https://crm.partner.example/mcp");

    @Test
    void createsAuthorizationHeaderOnlyForExactAudienceAndSufficientScope() {
        McpRemoteCredential credential = credential();

        assertThat(credential.authorizationHeadersFor(
                MCP_ENDPOINT, Set.of("crm:read")))
                .containsEntry("Authorization", "Bearer live-secret-token");
    }

    @Test
    void rejectsWrongAudienceOrInsufficientScope() {
        McpRemoteCredential credential = credential();

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
    void keepsTokenOutOfToStringAndBuildsTransportWithSafeFlags() {
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
    }

    @Test
    void rejectsRemoteRegistrationWhoseHostDoesNotMatchItsAudience() {
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
