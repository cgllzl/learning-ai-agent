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
import org.junit.jupiter.api.DisplayName;
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

/**
 * Day 5 兼容性测试。
 * 学习时重点看第二个方法：它真实截获了 LangChain4j 发出的 initialize 请求。
 */
class McpCompatibilityTest {

    @Test
    @DisplayName("目的：核对当前依赖仍处于 2025 旧线，并明确标记暂不支持的 2026 能力")
    void currentDependenciesStayOn2025AndMark2026FeaturesUnsupported() {
        McpCompatibilityMatrix.ProjectMcpStack stack = McpCompatibilityMatrix.currentProject();

        assertThat(stack.langChain4jMcpVersion()).isEqualTo("1.18.1-beta28");
        assertThat(stack.langChain4jProtocol()).isEqualTo("2025-11-25");
        assertThat(stack.officialJavaSdkVersion()).isEqualTo("2.0.1");
        assertThat(stack.officialJavaSdkProtocol()).isEqualTo("2025-11-25");
        assertThat(stack.supportsModernProtocolEndToEnd()).isFalse();
        assertThat(McpCompatibilityMatrix.modernClientApiAvailable()).isFalse();

        // 反射读取 SDK 实际导出的版本常量，避免看到“SDK 2.0.1”就误认为它支持协议 2026。
        assertThat(Arrays.stream(ProtocolVersions.class.getFields())
                .filter(field -> field.getType() == String.class)
                .map(this::readStringConstant))
                .contains(ProtocolVersions.MCP_2025_11_25)
                .doesNotContain(McpCompatibilityMatrix.MODERN_PROTOCOL);

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
    @DisplayName("目的：截获真实 initialize 请求，证明 LangChain4j Client 实际发送 2025-11-25")
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
