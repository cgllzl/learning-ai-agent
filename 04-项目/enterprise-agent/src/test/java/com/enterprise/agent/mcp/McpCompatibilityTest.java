package com.enterprise.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.enterprise.agent.security.mcp.McpCompatibilityMatrix;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.protocol.McpInitializeParams;
import dev.langchain4j.mcp.protocol.McpInitializeRequest;
import io.modelcontextprotocol.spec.ProtocolVersions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 把“规范最新版本”和“当前 Java 依赖实际支持版本”分开验证。 */
class McpCompatibilityTest {

    @Test
    void currentProjectRemainsOnLegacyProtocolUntilBothClientAndServerSupportModernEra() {
        McpCompatibilityMatrix.ProjectMcpStack stack = McpCompatibilityMatrix.currentProject();

        assertThat(stack.langChain4jMcpVersion()).isEqualTo("1.18.1-beta28");
        assertThat(stack.langChain4jProtocol()).isEqualTo("2025-11-25");
        assertThat(stack.officialJavaSdkVersion()).isEqualTo("2.0.1");
        assertThat(stack.officialJavaSdkProtocol()).isEqualTo("2025-11-25");
        assertThat(stack.supportsModernProtocolEndToEnd()).isFalse();
        assertThat(McpCompatibilityMatrix.modernClientApiAvailable()).isFalse();
    }

    @Test
    void officialJavaSdkDoesNotYetDeclareThe2026ProtocolConstant() {
        // 反射读取 SDK 实际导出的版本常量，避免看到“SDK 2.0.1”就误认为它支持协议 2026。
        assertThat(Arrays.stream(ProtocolVersions.class.getFields())
                .filter(field -> field.getType() == String.class)
                .map(this::readStringConstant))
                .contains(ProtocolVersions.MCP_2025_11_25)
                .doesNotContain(McpCompatibilityMatrix.MODERN_PROTOCOL);
    }

    @Test
    void modernOnlyFeaturesAreExplicitlyMarkedUnsupported() {
        List<McpCompatibilityMatrix.FeatureSupport> modernOnly =
                McpCompatibilityMatrix.features().stream()
                .filter(feature -> feature.feature().contains("server/discover")
                        || feature.feature().contains("MRTR")
                        || feature.feature().contains("ttlMs")
                        || feature.feature().contains("Tasks"))
                .toList();

        assertThat(modernOnly).hasSize(4)
                .allMatch(feature -> feature.support()
                        == McpCompatibilityMatrix.Support.UNSUPPORTED);
    }

    @Test
    void langChain4jClientActuallySends2025ProtocolInItsInitializeRequest() throws Exception {
        McpTransport transport = mock(McpTransport.class);
        JsonNode initializeResponse = new ObjectMapper().readTree("""
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "result": {
                    "protocolVersion": "2025-11-25",
                    "capabilities": {"tools": {"listChanged": false}},
                    "serverInfo": {"name": "compatibility-test", "version": "1.0.0"}
                  }
                }
                """);
        when(transport.initialize(any(McpInitializeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(initializeResponse));

        McpClient client = DefaultMcpClient.builder()
                .key("compatibility-test")
                .transport(transport)
                .autoHealthCheck(false)
                .build();
        try {
            ArgumentCaptor<McpInitializeRequest> captor =
                    ArgumentCaptor.forClass(McpInitializeRequest.class);
            verify(transport).initialize(captor.capture());
            McpInitializeParams params = (McpInitializeParams) captor.getValue().getParams();

            assertThat(params.getProtocolVersion()).isEqualTo("2025-11-25");
        } finally {
            client.close();
        }
    }

    private String readStringConstant(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("无法读取 MCP 协议版本常量", e);
        }
    }
}
