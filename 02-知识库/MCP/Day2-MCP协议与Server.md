# Day 2：MCP 协议概念 + 跑通一个 MCP Server

> Week 4 ｜ 归档：`05-记录/归档/2026-08-24-Week4-Day2-MCP协议与Server.md` ｜ 代码：`com.enterprise.agent.mcp.SimpleMcpServer`

## 一、MCP 是什么

**MCP（Model Context Protocol）**：让大模型/Agent 用一套统一协议连接外部工具和数据源的标准。可以类比「USB-C」——以前每种工具都要单独写适配器，现在只要支持 MCP 就能即插即用。

解决的核心痛点：**N 个模型 × M 个工具 = N×M 种对接**，改成 MCP 后变成 **N + M**。

## 二、核心概念

| 概念 | 说明 |
| --- | --- |
| Client（客户端） | 发起方，通常是 Agent / AI 应用 |
| Server（服务端） | 暴露能力的一方（工具/数据） |
| Tools | 可调用的函数（带 JSON Schema 描述） |
| Resources | 可读取的数据（文档、配置等，有 URI） |
| Prompts | 可复用的提示词模板 |

- **通信**：基于 **JSON-RPC 2.0**（请求/响应/通知三种消息）。
- **生命周期**：initialize → 能力协商 → 正常运行 → shutdown。
- **授权**：由宿主（Host）负责用户授权，Server 可声明需要哪些权限。

## 三、什么时候该用 / 不该用

- ✅ 该用：工具要跨多个 AI 应用复用；团队统一管理工具集；需要 Resources/Prompts 的标准化。
- ❌ 不该用：只给自己一个应用用、工具很少、且不需要协议级复用——直接写 @Tool 更简单。

## 四、跑通一个 MCP Server（官方 Java SDK）

依赖：`io.modelcontextprotocol.sdk:mcp:2.0.1`（核心在 mcp-core，JSON 用 mcp-json-jackson3）。

```java
McpJsonMapper mapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());
StdioServerTransportProvider transport = new StdioServerTransportProvider(
        mapper, new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());

McpSyncServer server = McpServer.sync(transport)
        .serverInfo("enterprise-tools", "1.0.0")     // 服务端信息
        .toolCall(addTool(), this::add)               // 注册工具 + 处理函数
        .build();
```

工具定义（name + description + JSON Schema 入参）：

```java
McpSchema.Tool tool = McpSchema.Tool.builder("add")
        .description("计算两个数字之和")
        .inputSchema(McpSchema.JsonSchema.builder()
                .type("object")
                .properties(Map.of("a", Map.of("type", "number"), "b", Map.of("type", "number")))
                .required(List.of("a", "b"))
                .build())
        .build();
```

处理函数（读入参 → 计算 → 返回文本结果）：

```java
private McpSchema.CallToolResult add(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
    int a = ((Number) request.arguments().get("a")).intValue();
    int b = ((Number) request.arguments().get("b")).intValue();
    return McpSchema.CallToolResult.builder(
            List.of(new McpSchema.TextContent(String.valueOf(a + b)))).build();
}
```

## 五、验证

- `SimpleMcpServerTest`（2 个）：server 成功注册 `add` 工具（`listTools` 含 add）、`add(2,3)=5`。
- 说明：Day 2 只做 Server 侧「构建 + 注册 + 工具逻辑」；真正跨进程/跨端调用由 Day 3 的 LangChain4j McpClient 完成。

## 六、Day 2 完成标准

- [x] 理解 MCP 协议概念（Client/Server/Tools/Resources/Prompts/JSON-RPC/生命周期/授权）
- [x] 跑通一个 MCP Server（注册工具并验证逻辑）