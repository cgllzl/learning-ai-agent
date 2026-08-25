# Day 4：把一个现有 Tool 改造成 MCP Tool

> Week 4 ｜ 归档：`05-记录/归档/2026-08-25-Week4-Day4-改造MCPTool.md` ｜ 代码：`com.enterprise.agent.mcp.OrderMcpServer`

## 一、先弄清楚：这次要解决什么

前面几周，我们已经有一个能干的 `OrderTools`，里面的 `getOrder` 是标准的 LangChain4j 写法：

```java
@Tool("根据订单号查询订单信息")
public String getOrder(@P("订单号，例如 O1001") String orderId) {
    return data.findOrderById(orderId) ... ;
}
```

这套东西**只能给 LangChain4j 的 AiServices 用**。换句话说，它像是一张「本店会员卡」——只有我们自己店里的 Agent 刷得了。

现在的问题来了：如果别的团队、别的 AI 应用（比如一个完全不用 LangChain4j 的系统）也想用「查订单」这个能力，怎么办？答案是给它换成一张「国际通用的标准卡」——**MCP Tool**。改造不是重写业务逻辑，而是给同一个能力换一种"对外说明的方式"。

## 二、一个贯穿全篇的类比

把「工具」想象成一家餐厅的菜：

- `@Tool` 写法 = 你走进自己熟悉的餐厅，直接对厨师喊「来一份查订单，参数 O1001」。厨师是你的人，他知道你的暗号。
- `MCP Tool` 写法 = 你给这家菜印了一张标准菜单：菜名、口味描述、需要哪些食材（参数），挂在所有持标准会员卡的顾客面前。谁都能照着菜单点，厨师做的还是那道菜。

所以改造的真相就是：**把「厨师听得懂的暗号」翻译成「人人都看得懂的菜单」，厨师不动。**

## 三、改造前后对照

| | @Tool 写法 | MCP Tool 写法 |
| --- | --- | --- |
| 工具名 | 方法名 `getOrder` | `Tool.builder("getOrder")` |
| 工具描述 | `@Tool("根据订单号查询订单信息")` | `.description("根据订单号查询订单信息")` |
| 参数说明 | `@P("订单号，例如 O1001")` | JSON Schema 里的 `properties` + `description` |
| 参数类型 | Java 类型 `String` | Schema 里的 `"type": "string"` |
| 谁执行 | 方法本体直接跑 | `handler` 函数里调同一个方法 |

看，两边信息其实一模一样，只是换了一套"语言"。

## 四、一步步动手（完整代码带解释）

### 第 1 步：把参数说明翻译成 JSON Schema

`@P("订单号，例如 O1001")` 之前只给 LangChain4j 看，现在要变成所有 MCP 客户端都能读懂的"参数身份证"：

```java
McpSchema.JsonSchema schema = McpSchema.JsonSchema.builder()
        .type("object")                        // 入参整体是一个对象
        .properties(Map.of(
                "orderId", Map.of(
                        "type", "string",               // 参数类型
                        "description", "订单号，例如 O1001")))  // 参数说明
        .required(List.of("orderId"))           // 哪些必填
        .build();
```

逐行理解：
- `type("object")`：工具参数不是孤零零的一个值，而是包在一个对象里。
- `properties(...)`：这个对象里有哪些字段、每个字段长什么样。
- `required(...)`：哪些字段必须给，少一个客户端就会报错。
- 这份 Schema 就等价于 `@P` 的"参数说明"，只是写得更正式、机器可读。

### 第 2 步：给工具做一张"菜单卡片"

```java
McpSchema.Tool tool = McpSchema.Tool.builder("getOrder")
        .description("根据订单号查询订单信息")
        .inputSchema(schema)      // 刚才那份参数身份证
        .build();
```

- `builder("getOrder")`：起工具名。
- `.description(...)`：对应原来的 `@Tool(...)` 描述。
- `.inputSchema(schema)`：挂上参数说明。

### 第 3 步：写 handler——真正的业务逻辑

MCP 工具被调用时，会进到这个 handler。**业务逻辑仍然复用原来的 `getOrder`**，只是入口换了一层：

```java
private McpSchema.CallToolResult getOrder(
        McpSyncServerExchange exchange,     // 会话上下文，这里用不到
        McpSchema.CallToolRequest request   // 客户端发来的调用请求
) {
    String orderId = (String) request.arguments().get("orderId");  // 取出参数
    String text = orderTools.getOrder(orderId);                     // 复用旧逻辑
    return McpSchema.CallToolResult.builder(
            List.of(new McpSchema.TextContent(text))).build();      // 包成结果返回
}
```

三个要点：
- `request.arguments()` 是一个 `Map<String, Object>`，参数全在这里，取的时候要强转 `(String)`。
- 中间那一行 `orderTools.getOrder(orderId)` 就是周二的旧方法，一个字没改。
- 最后必须把结果包成 `CallToolResult`，因为 MCP 协议只认这种格式。

### 第 4 步：注册进 Server

```java
McpSyncServer server = McpServer.sync(transport)
        .serverInfo("enterprise-order-tools", "1.0.0")
        .toolCall(tool, this::getOrder)   // 菜单卡片 + 干活函数
        .build();
```

`toolCall(工具说明, 处理函数)` 一次注册一个工具。到这里，`getOrder` 就正式"加入 MCP 会员体系"了。

## 五、为什么值得费这一层功夫

改造前，`getOrder` 只能在 LangChain4j 内部流通；改造后，任何实现了 MCP Client 的系统——不管它用什么语言、什么框架——都能发现并调用它。**你的业务逻辑写一次，能力对外暴露一次，别人接入不需要再找你改代码。** 这就是 Day 2 讲的「N + M」价值落地的具体姿势。

## 六、验证

`OrderMcpServerTest`（2 个用例）：
1. Server 的 `listTools()` 里能查到 `getOrder`（说明工具注册成功）。
2. `server.getOrder("O1001")` 返回内容包含 `O1001` 和 `399.0`（说明业务逻辑完整复用）。

## 七、Day 4 完成标准

- [x] 把一个现有 Tool（getOrder）改造成 MCP Tool

## 八、补上缺口：让大模型真的用上它

光把工具挂上 MCP Server 还不够，你可能马上会问：「那大模型怎么用上它？」答案是再接一层：**McpToolProvider + McpClient + AiServices**。

完整链路：

```mermaid
flowchart LR
    L["DeepSeek 大模型"] --> A["AiServices Agent"]
    A --> P["McpToolProvider"]
    P --> C["McpClient"]
    C --> T["MCP 工具 getOrder"]
```

```java
// 1) 内存版 McpClient：实现 LangChain4j 的 McpClient 接口，把工具"交出来"
//    这里省略了真实传输（stdio/http），直接委托给 OrderTools，便于演示
McpToolProvider toolProvider = McpToolProvider.builder()
        .mcpClients(new InMemoryMcpClient(new OrderTools(new MockOrderData())))
        .build();

// 2) 把 MCP 工具接进 Agent（toolProvider 和普通 @Tool 一样用）
McpOrderAssistant assistant = AiServices.builder(McpOrderAssistant.class)
        .chatModel(chatModel)
        .toolProvider(toolProvider)
        .build();

// 3) 自然语言提问 → 模型自己决定调 getOrder
String reply = assistant.chat("查询订单 O1001 的信息");
```

- `InMemoryMcpClient` 是我们的「内存版 MCP 客户端」：它实现了 `McpClient` 接口（`listTools` / `executeTool`），但跳过了 JSON-RPC 传输，直接把调用委托给本地 `OrderTools`。这是为了在学习环境里看清整条链路；真实环境把它换成 `StdioMcpTransport` 或 `HttpMcpTransport` 即可。
- 关键验证点：模型回复里的 `399` 只可能来自 `getOrder` 的返回值，说明**模型真的通过 MCP 通道调用了工具**，不是凭空编的。

### 验证

- `InMemoryMcpClientTest`（2 个）：能列出 `getOrder`、能执行并返回订单数据。
- `McpOrderLiveTest`（真实 DeepSeek）：问「查询订单 O1001」→ 回复带出 O1001 和 399，联动链路实测通过。

## 九、如何本地测试

1. 接线测试（无需 Key，最快）：
```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent
mvn test -Dtest=InMemoryMcpClientTest,OrderMcpServerTest,McpToolProviderTest,SimpleMcpServerTest
```

2. 真实 LLM 联动（需要 DeepSeek Key，走 MCP 通道）：
```powershell
.\scripts\test-live.ps1 -Test McpOrderLiveTest
```
脚本自动读 `.env` 的 Key，输出里看 `[MCP 联动回答]` 是否包含 `O1001` 和 `399`。

3. IDEA 里右键测试类 Run；`McpOrderLiveTest` 需要在 Run Configuration 的 Environment variables 里配 `DEEPSEEK_API_KEY`。
