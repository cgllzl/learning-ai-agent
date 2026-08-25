package com.enterprise.agent.mcp;

import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderTools;
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
 * 把一个现有的 LangChain4j @Tool（OrderTools.getOrder）改造成 MCP Tool。
 * 核心思路：@Tool 的「方法描述 + 参数说明」翻译成 MCP 的「Tool 描述 + JSON Schema」，
 * 真正的业务逻辑仍然复用原来的 OrderTools，不重写。
 */
public class OrderMcpServer {

    private final McpSyncServer server;
    private final OrderTools orderTools;

    public OrderMcpServer() {
        this(new OrderTools(new MockOrderData()));
    }

    public OrderMcpServer(OrderTools orderTools) {
        this.orderTools = orderTools;

        McpJsonMapper mapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());
        StdioServerTransportProvider transport = new StdioServerTransportProvider(
                mapper, new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());

        this.server = McpServer.sync(transport)
                .serverInfo("enterprise-order-tools", "1.0.0")
                .toolCall(orderTool(), this::getOrder)
                .build();
    }

    public List<McpSchema.Tool> listTools() {
        return server.listTools();
    }

    /** 复用现有业务逻辑（等价于 @Tool 的 getOrder 方法本体）。 */
    public String getOrder(String orderId) {
        return orderTools.getOrder(orderId);
    }

    private McpSchema.Tool orderTool() {
        McpSchema.JsonSchema schema = McpSchema.JsonSchema.builder()
                .type("object")
                .properties(Map.of(
                        "orderId", Map.of("type", "string", "description", "订单号，例如 O1001")))
                .required(List.of("orderId"))
                .build();
        return McpSchema.Tool.builder("getOrder")
                .description("根据订单号查询订单信息")
                .inputSchema(schema)
                .build();
    }

    private McpSchema.CallToolResult getOrder(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        String orderId = (String) request.arguments().get("orderId");
        String text = getOrder(orderId);
        return McpSchema.CallToolResult.builder(List.of(new McpSchema.TextContent(text))).build();
    }
}