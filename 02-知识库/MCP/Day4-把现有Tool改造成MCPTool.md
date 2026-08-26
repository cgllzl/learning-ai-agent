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

### 真正跑通的写法（`McpOrderLiveTest` 现在用的）

```java
McpClient mcpClient = startOrderMcpServer();   // 关键：先拉起一个真实的 Server 子进程
try {
    McpToolProvider toolProvider = McpToolProvider.builder()
            .mcpClients(mcpClient)
            .build();

    McpOrderAssistant assistant = AiServices.builder(McpOrderAssistant.class)
            .chatModel(chatModel)
            .toolProvider(toolProvider)
            .build();

    String reply = assistant.chat("查询订单 O1001 的信息");
} finally {
    mcpClient.close();
}
```

这里的 `startOrderMcpServer()` 是测试类里自己写的一个小助手方法，不是框架自带的东西。它做的事一句话就能讲清楚：**拼一条命令，让 `StdioMcpTransport` 启动一个独立的 `OrderMcpServer` 进程**。

```java
private static McpClient startOrderMcpServer() {
    String javaBin = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
    List<String> command = List.of(
            javaBin,                       // 用当前 JDK 里的 java.exe
            "-cp",                         // classpath 参数
            resolveClasspath(),            // 告诉新进程去哪里找类
            OrderMcpServer.class.getName() // 要启动的主类
    );

    return DefaultMcpClient.builder()
            .key("order-mcp-stdio")
            .transport(StdioMcpTransport.builder().command(command).build())
            .build();
}
```

而 `resolveClasspath()` 则回答另一个问题：新开的 JVM 是“另一台空电脑”，它不知道你的代码和依赖在哪，所以要给它一份完整的 classpath。

```java
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
```

所以不用自己去数“要启动哪些类”，真正需要记住的只有两类测试命令，子进程是测试自动帮你开、用完自动帮你关的。

### 内存版 Client 还留着干嘛

项目里确实还有一个 `InMemoryMcpClient`，它实现了 `McpClient` 接口（`listTools` / `executeTool`），但跳过 JSON-RPC 传输，直接把调用委托给本地 `OrderTools`。它现在不是主角，只承担两个职责：

- 在 `InMemoryMcpClientTest` 这种底层单测里，快速验证「工具列表、参数解析、结果封装」。
- 帮初学者先看清「大模型 → AiServices → McpToolProvider → McpClient → 工具」这条链路的形状。

真实端到端验证以 `StdioMcpTransport` 为准，`McpOrderLiveTest` 用的就是它，所以模型拿到的 `399` 是真的一路走完协议链路、由 Server 进程返回的。

关键验证点：模型回复里的 `399` 只可能来自 `getOrder` 的返回值，说明**模型真的通过 MCP 协议调用了工具**，不是凭空编的。

### 验证

- `InMemoryMcpClientTest`（2 个）：能列出 `getOrder`、能执行并返回订单数据。
- `OrderMcpServerStdioTest`（2 个，无需 DeepSeek Key）：真实启动 `OrderMcpServer` 子进程，用 `DefaultMcpClient + StdioMcpTransport` 列出工具、执行工具，验证 MCP 协议链路通。
- `McpOrderLiveTest`（真实 DeepSeek）：问「查询订单 O1001」→ 回复带出 O1001 和 399，走真实 stdio 的联动链路实测通过。

## 九、如何本地测试（先分清哪些类要你启动）

先说结论：**这几个类都不需要你手动启动**。你只需要在 PowerShell 里跑一条 Maven 命令，测试自己会开 Server 子进程、连上、用完再关掉。

先认识一下这些类各自的角色，就不会觉得“怎么这么多类要启动”：

| 类 | 角色 | 需要你手动启动吗 |
| --- | --- | --- |
| `OrderMcpServer` | MCP Server 本体，里面带 `main()` | 不用，测试会把它当成子进程启动 |
| `McpOrderLiveTest` | 真实联调：开 Server + DeepSeek，让大模型通过 MCP 查订单 | 不用，Maven 跑测试即可 |
| `OrderMcpServerStdioTest` | 不开大模型，只验证 Server 进程能列工具、能执行工具 | 不用 |
| `InMemoryMcpClientTest` / `OrderMcpServerTest` / `McpToolProviderTest` | 更底层的单测，验证工具注册、参数解析、结果封装 | 不用 |

按“从快到慢、从不需要 Key 到需要 Key”的顺序测：

1. 真实 stdio 链路（推荐先跑，无需 DeepSeek Key，但会真的启动 Server 子进程）：
```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent
mvn test -Dtest=OrderMcpServerStdioTest
```

2. 真实 LLM 联动（需要 DeepSeek Key，走真实 MCP stdio 通道）：
```powershell
.\scripts\test-live.ps1 -Test McpOrderLiveTest
```
脚本自动读 `.env` 的 Key，输出里看 `[MCP stdio 联动回答]` 是否包含 `O1001` 和 `399`。

3. 只想跑底层接线测试（无需 Key，最快）：
```powershell
mvn test -Dtest=InMemoryMcpClientTest,OrderMcpServerTest,McpToolProviderTest,SimpleMcpServerTest
```

4. 在 IDEA 里测试：直接右键测试类 Run；`McpOrderLiveTest` 需要在 Run Configuration 的 Environment variables 里配 `DEEPSEEK_API_KEY`。

## 十、真实 stdio 的两个坑（Day 4 补记）

把测试从「内存版 Client」升级到「真实 stdio」时，会撞上两个很容易卡死/串数据的坑，这里记下来：

### 1. stdout 只能传输 JSON-RPC，日志必须去 stderr

MCP 的 stdio 传输规定：`stdin` 收请求、`stdout` 回响应，而且 `stdout` 上每一行都必须是 JSON-RPC 报文。可 Java 的 Logback 默认把 INFO 日志也打到 `stdout`，于是服务端刚启动，日志就和协议报文混在一起，客户端会报：

```text
Ignoring message received because it is not valid JSON: ...
```

解决办法是给 Server 子进程单独配一份 `logback-mcp.xml`，把 `ConsoleAppender` 的 `target` 设成 `System.err`。这样日志走错误流，协议流干干净净。`OrderMcpServer.main()` 里已经通过 `logback.configurationFile` 自动指定了这份配置。

### 2. Server 的 main 不能 `join()` 死等

一开始我很自然地在 `main()` 最后写：

```java
Thread.currentThread().join(); // 让主线程一直等，进程别退出
```

结果客户端 `close()` 时会卡在关闭 stdout 流这一步：库会先关 IO 流、再销毁进程，而 `join()` 让 Server 进程永远活着，导致「关流等进程退出 / 进程等流关闭」互相等死。

正确做法是**让 main 正常返回**。Server 的收包线程是非守护线程，它会维持 JVM 存活；客户端关闭 stdin 后，收包线程读到 EOF 自动退出，进程也就自然结束。
