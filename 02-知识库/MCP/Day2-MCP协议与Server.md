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

## 七、新语法通俗讲解

### 1. 链式调用（Builder / Fluent API）
`McpServer.sync(transport).serverInfo(...).toolCall(...).build()` 这种「一路点下去」的写法叫链式调用：每个方法都返回自己（return this），所以能连续调用，最后 `build()` 才真正组装出对象。类比点菜：你一项项勾选（serverInfo、toolCall），最后说「下单」（build）。

### 2. `Map.of(...)` 和 `List.of(...)`
Java 9 之后快速造「写死不动」的集合：
```java
Map.of("a", 1, "b", 2);      // 等价于一个只有 a/b 两个键的 Map
List.of("x", "y");           // 两个元素的 List
```
好处是省去 `new HashMap` + 一次次 put；代价是这个集合不可修改。适合写配置、写测试数据。

### 3. `McpSchema.Tool.builder("add")`
`builder("add")` 是给工具**起名字**（"add"），后面的 `.description()` 写**它是干嘛的**，`.inputSchema()` 写**它要什么参数**，`build()` 收工。这个 Tool 对象就是交给模型看的「工具说明书」。

### 4. JSON Schema：参数的身份证
```java
McpSchema.JsonSchema.builder()
    .type("object")                              // 入参整体是个对象
    .properties(Map.of("a", Map.of("type", "number")))  // 有哪些字段、各自什么类型
    .required(List.of("a"))                      // 哪些必填
    .build();
```
模型就是靠这份 Schema 知道「这个工具有参数 a、b，都是数字」才能填对参数。不用手写 JSON 字符串，用 Builder 拼。

### 5. 处理函数：`(exchange, request) -> { ... }`
这是一个 **Lambda 表达式**，等价于一个匿名函数。它接收两个参数：
- `exchange`：会话上下文（Day 2 用不上，先忽略）
- `request`：客户端发来的调用请求

返回值是 `CallToolResult`（调用结果）。

### 6. 从请求里取参数
```java
((Number) request.arguments().get("a")).intValue()
```
- `request.arguments()` 返回 `Map<String, Object>`——参数都是 Object 类型。
- `(Number)` 是**强制类型转换**：告诉编译器「它其实是数字」。
- `.intValue()` 把数字转成 int。

### 7. 把结果包成 MCP 要求的格式
```java
McpSchema.CallToolResult.builder(List.of(new McpSchema.TextContent("5"))).build();
```
- `TextContent("5")`：把文本包成 MCP 认识的「内容」。
- `List.of(...)`：内容是一个列表（可有多段）。
- `CallToolResult.builder(列表).build()`：包成调用结果返回。

### 8. 方法引用 `this::add`
`this::add` 是 Lambda 的简写：`(exchange, request) -> add(exchange, request)` 可以缩写成 `this::add`，只要方法签名对得上。

### 9. `record`（前几周已出现）
`Tool`、`JsonSchema`、`CallToolResult` 都是 record：不可变数据类，字段自动生成访问方法（如 `tool.name()`）。不用手写 getter/setter。

## 八、SimpleMcpServer 构造方法参数讲解

```java
// 1) 配"翻译官"：JSON 和 Java 对象互转
McpJsonMapper mapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());

// 2) 配"听筒 + 话筒"：stdio 通过输入/输出流收发消息
StdioServerTransportProvider transport = new StdioServerTransportProvider(
        mapper,                                  // 上面的翻译官
        new ByteArrayInputStream(new byte[0]),   // 输入流（听筒），空数据占位
        new ByteArrayOutputStream());            // 输出流（话筒），消息先攒在这里

// 3) 组装 Server + 注册工具
this.server = McpServer.sync(transport)
        .serverInfo("enterprise-tools", "1.0.0")  // 自我介绍：名字 + 版本
        .toolCall(addTool(), this::add)           // 注册工具：说明书 + 干活函数
        .build();                                 // 真正组装出 McpSyncServer
```

- `JacksonMcpJsonMapper(JsonMapper.builder().build())`：Jackson 负责 JSON 编解码；builder→build 是 Jackson 的惯用写法。
- `StdioServerTransportProvider(mapper, in, out)`：stdio 传输，通过输入流收消息、输出流发消息；真实运行时应传 `System.in` / `System.out`，学习期用空流占位。
- `.serverInfo(name, version)`：握手时告诉客户端"我是谁"。
- `.toolCall(工具说明书, 处理函数)`：注册工具，说明书是给模型看的，处理函数是真正执行的。
- `.build()`：把前面配置组装成可用的 `McpSyncServer`。

## 企业落地案例
- 场景：企业内部不同团队（订单、物流、CRM）各自维护自己的 MCP Server，统一接入后，任何 AI 应用都能发现并调用这些工具。
- 真实联调：`.\scripts\test-live.ps1 -Test McpOrderLiveTest`，验证跨进程 MCP 工具被真实调用。
