package com.enterprise.agent.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端联调：真实启动一个 MCP Server 子进程（stdio），
 * 大模型再通过 DefaultMcpClient + StdioMcpTransport 通过 MCP 协议调用 getOrder。
 * 需设置 DEEPSEEK_API_KEY：
 *   $env:DEEPSEEK_API_KEY="..." ; mvn test -Dtest=McpOrderLiveTest -Dsurefire.useFile=false
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class McpOrderLiveTest {

    @Test
    void llmCallsMcpTool() throws Exception {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        McpClient mcpClient = startOrderMcpServer();
        try {
            // 把「真正的 MCP Client」桥接进 Agent
            McpToolProvider toolProvider = McpToolProvider.builder()
                    .mcpClients(mcpClient)
                    .build();

            McpOrderAssistant assistant = AiServices.builder(McpOrderAssistant.class)
                    .chatModel(model)
                    .toolProvider(toolProvider)
                    .build();

            String reply = assistant.chat("查询订单 O1001 的信息");
            System.out.println("[MCP stdio 联动回答] " + reply);

            // 金额只可能来自 MCP 工具返回，说明模型真的通过 MCP 协议调用了 getOrder
            assertThat(reply).contains("O1001");
            assertThat(reply).contains("399");
        } finally {
            mcpClient.close();
        }
    }

    /**
     * 用独立子进程启动 {@link OrderMcpServer}，再返回通过 stdio 连接它的 MCP Client。
     * 这样才真正走完「大模型 → AiServices → McpToolProvider → DefaultMcpClient
     * → JSON-RPC over stdio → OrderMcpServer → getOrder」整条链路。
     */
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
        // Maven Surefire 会提供完整的测试 classpath；IDEA 直接运行时退回到 java.class.path
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
