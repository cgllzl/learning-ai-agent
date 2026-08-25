package com.enterprise.agent.mcp;

import com.enterprise.agent.agent.OrderTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpPrompt;
import dev.langchain4j.mcp.client.McpReadResourceResult;
import dev.langchain4j.mcp.client.McpResource;
import dev.langchain4j.mcp.client.McpResourceTemplate;
import dev.langchain4j.mcp.client.McpRoot;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutionResult;

import java.util.List;
import java.util.Map;

/**
 * 内存版 MCP 客户端（Week 4 Day 4 补充）。
 * 实现 LangChain4j 的 McpClient 接口，把 MCP 工具暴露给 AiServices。
 * 传输层（JSON-RPC over stdio/http）在这里被省略，直接委托给本地业务逻辑，
 * 用于演示「大模型 → McpToolProvider → McpClient → 工具」这条完整链路。
 */
public class InMemoryMcpClient implements McpClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OrderTools orderTools;

    public InMemoryMcpClient(OrderTools orderTools) {
        this.orderTools = orderTools;
    }

    @Override
    public String key() {
        return "order-mcp";
    }

    @Override
    public List<ToolSpecification> listTools() {
        JsonObjectSchema parameters = JsonObjectSchema.builder()
                .addStringProperty("orderId", "订单号，例如 O1001")
                .required("orderId")
                .build();
        return List.of(ToolSpecification.builder()
                .name("getOrder")
                .description("根据订单号查询订单信息")
                .parameters(parameters)
                .build());
    }

    @Override
    public List<ToolSpecification> listTools(InvocationContext invocationContext) {
        return listTools();
    }

    @Override
    public ToolExecutionResult executeTool(dev.langchain4j.agent.tool.ToolExecutionRequest request) {
        return executeTool(request, null);
    }

    @Override
    public ToolExecutionResult executeTool(dev.langchain4j.agent.tool.ToolExecutionRequest request,
                                           InvocationContext invocationContext) {
        if (!"getOrder".equals(request.name())) {
            return ToolExecutionResult.builder().isError(true).resultText("未知工具: " + request.name()).build();
        }
        try {
            JsonNode json = OBJECT_MAPPER.readTree(request.arguments());
            String orderId = json.get("orderId").asText();
            return ToolExecutionResult.builder().resultText(orderTools.getOrder(orderId)).build();
        } catch (Exception e) {
            return ToolExecutionResult.builder().isError(true).resultText("工具调用失败: " + e.getMessage()).build();
        }
    }

    @Override
    public List<McpResource> listResources() {
        return List.of();
    }

    @Override
    public List<McpResource> listResources(InvocationContext invocationContext) {
        return List.of();
    }

    @Override
    public List<McpResourceTemplate> listResourceTemplates() {
        return List.of();
    }

    @Override
    public List<McpResourceTemplate> listResourceTemplates(InvocationContext invocationContext) {
        return List.of();
    }

    @Override
    public McpReadResourceResult readResource(String uri) {
        throw new UnsupportedOperationException("本客户端不支持 Resources");
    }

    @Override
    public McpReadResourceResult readResource(String uri, InvocationContext invocationContext) {
        return readResource(uri);
    }

    @Override
    public void subscribeToResource(String uri) {
    }

    @Override
    public void unsubscribeFromResource(String uri) {
    }

    @Override
    public List<McpPrompt> listPrompts() {
        return List.of();
    }

    @Override
    public McpGetPromptResult getPrompt(String name, Map<String, Object> arguments) {
        throw new UnsupportedOperationException("本客户端不支持 Prompts");
    }

    @Override
    public void checkHealth() {
    }

    @Override
    public void setRoots(List<McpRoot> roots) {
    }

    @Override
    public void close() {
    }
}