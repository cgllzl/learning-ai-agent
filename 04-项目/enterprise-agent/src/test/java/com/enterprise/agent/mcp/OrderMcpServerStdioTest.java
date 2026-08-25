package com.enterprise.agent.mcp;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolExecutionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 不依赖大模型的 MCP stdio 联通性测试：
 * 真实启动 {@link OrderMcpServer} 子进程，用 DefaultMcpClient 通过 JSON-RPC over stdio 调用它。
 */
class OrderMcpServerStdioTest {

    @Test
    @Timeout(30)
    void listsToolsOverRealStdio() throws Exception {
        McpClient client = startOrderMcpServer();
        try {
            var tools = client.listTools();
            assertThat(tools)
                    .anyMatch(tool -> "getOrder".equals(tool.name()));
        } finally {
            client.close();
        }
    }

    @Test
    @Timeout(30)
    void executesToolOverRealStdio() throws Exception {
        McpClient client = startOrderMcpServer();
        try {
            ToolExecutionResult result = client.executeTool(ToolExecutionRequest.builder()
                    .name("getOrder")
                    .arguments("{\"orderId\":\"O1001\"}")
                    .build());

            assertThat(result.isError()).isFalse();
            assertThat(result.resultText()).contains("O1001").contains("399");
        } finally {
            client.close();
        }
    }

    private static McpClient startOrderMcpServer() {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        List<String> command = List.of(
                javaBin,
                "-cp",
                resolveClasspath(),
                OrderMcpServer.class.getName());

        return DefaultMcpClient.builder()
                .key("order-mcp-stdio")
                .transport(StdioMcpTransport.builder().command(command).build())
                .build();
    }

    private static String resolveClasspath() {
        String classpath = System.getProperty("surefire.test.class.path");
        if (classpath == null || classpath.isBlank()) {
            classpath = System.getProperty("java.class.path");
        }
        if (classpath == null || classpath.isBlank()) {
            classpath = "target/classes;target/test-classes";
        }
        return classpath;
    }
}
