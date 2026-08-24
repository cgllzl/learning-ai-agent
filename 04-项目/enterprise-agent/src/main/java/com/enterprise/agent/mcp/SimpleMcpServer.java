package com.enterprise.agent.mcp;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * 一个最小的 MCP Server（Week 4 Day 2，官方 MCP Java SDK + stdio）。
 * 暴露一个工具：add(a, b) 两数相加。
 * Day 3 会通过 LangChain4j 的 McpClient 真正调用它。
 */
public class SimpleMcpServer {

    private final McpSyncServer server;

    public SimpleMcpServer() {
        McpJsonMapper mapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());
        StdioServerTransportProvider transport = new StdioServerTransportProvider(
                mapper, new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());

        this.server = McpServer.sync(transport)
                .serverInfo("enterprise-tools", "1.0.0")
                .toolCall(addTool(), this::add)
                .build();
    }

    public List<McpSchema.Tool> listTools() {
        return server.listTools();
    }

    /** 工具的核心逻辑（与 MCP 协议解耦，方便单独测试）。 */
    public int add(int a, int b) {
        return a + b;
    }

    private McpSchema.Tool addTool() {
        McpSchema.JsonSchema schema = McpSchema.JsonSchema.builder()
                .type("object")
                .properties(Map.of(
                        "a", Map.of("type", "number", "description", "第一个加数"),
                        "b", Map.of("type", "number", "description", "第二个加数")))
                .required(List.of("a", "b"))
                .build();
        return McpSchema.Tool.builder("add")
                .description("计算两个数字之和")
                .inputSchema(schema)
                .build();
    }

    private McpSchema.CallToolResult add(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        int a = ((Number) request.arguments().get("a")).intValue();
        int b = ((Number) request.arguments().get("b")).intValue();
        String text = String.valueOf(add(a, b));
        return McpSchema.CallToolResult.builder(List.of(new McpSchema.TextContent(text))).build();
    }
}