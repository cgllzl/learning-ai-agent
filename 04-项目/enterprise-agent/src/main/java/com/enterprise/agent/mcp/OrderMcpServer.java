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
import java.io.InputStream;
import java.io.OutputStream;
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
        this(orderTools, new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
    }

    private OrderMcpServer(OrderTools orderTools, InputStream inputStream, OutputStream outputStream) {
        this.orderTools = orderTools;

        McpJsonMapper mapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());
        StdioServerTransportProvider transport = new StdioServerTransportProvider(
                mapper, inputStream, outputStream);

        this.server = McpServer.sync(transport)
                .serverInfo("enterprise-order-tools", "1.0.0")
                .toolCall(orderTool(), this::getOrder)
                .build();
    }

    /**
     * 以真实的 stdio 方式启动：stdin/stdout 对接 MCP 客户端。
     * 供子进程方式启动（例如 {@code java com.enterprise.agent.mcp.OrderMcpServer}）。
     */
    public static OrderMcpServer stdio() {
        return new OrderMcpServer(new OrderTools(new MockOrderData()), System.in, System.out);
    }

    public static void main(String[] args) {
        // stdio 的 stdout 只能用来传输 JSON-RPC，因此把日志改到 stderr，避免污染协议流
        System.setProperty("logback.configurationFile", "logback-mcp.xml");
        stdio();
        // 不要 join()：stdio 的收包线程是非守护线程，会维持 JVM 存活；
        // 客户端关闭 stdin 后收包线程退出，进程随之自然结束。
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
