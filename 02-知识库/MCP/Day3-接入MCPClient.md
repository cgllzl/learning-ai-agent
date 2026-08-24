# Day 3：在 Java 项目里接入 MCP Client（LangChain4j）

> Week 4 ｜ 归档：`05-记录/归档/2026-08-24-Week4-Day3-接入MCPClient.md` ｜ 代码：`com.enterprise.agent.mcp`

## 一、接入整体图

```mermaid
flowchart LR
    A["AiServices Agent"] --> P["McpToolProvider"]
    P --> C["McpClient"]
    C --> T["Transport 传输"]
    T --> S["MCP Server"]
```

- Agent 想用 MCP 工具 → 通过 `McpToolProvider` 把 MCP 工具"翻译"成 LangChain4j 认识的 Tool → 再通过 `McpClient` 和传输层真正调用远程 Server。

## 二、三个关键组件

| 组件 | 作用 | 关键类 |
| --- | --- | --- |
| McpClient | 客户端，负责 listTools / callTool | `DefaultMcpClient` |
| Transport | 传输方式（stdio 子进程 / http / websocket） | `StdioMcpTransport`、`HttpMcpTransport` |
| McpToolProvider | 把 MCP 工具暴露给 AiServices | `McpToolProvider.builder()` |

## 三、接入代码

### 1. 创建 McpClient（stdio 传输，指向一个 MCP Server 进程）

```java
McpTransport transport = StdioMcpTransport.builder()
        .command(List.of("java", "-jar", "mcp-server.jar"))   // 用子进程启动 Server
        .build();

McpClient mcpClient = DefaultMcpClient.builder()
        .transport(transport)
        .build();
```

- `StdioMcpTransport`：通过子进程的 stdin/stdout 和 Server 通信，`command` 就是要执行的启动命令。

### 2. 把 MCP 工具暴露给 Agent（关键桥接）

```java
McpToolProvider toolProvider = McpToolProvider.builder()
        .mcpClients(List.of(mcpClient))   // 可以接多个 Server
        .build();

OrderAssistant assistant = AiServices.builder(OrderAssistant.class)
        .chatModel(chatModel)
        .tools(toolProvider)              // 和普通 @Tool 一样使用
        .build();
```

- `McpToolProvider` 实现了 LangChain4j 的 `ToolProvider` 接口，所以能直接塞进 `AiServices.tools(...)`。
- 一个 provider 可以挂多个 McpClient（连多个 MCP Server），Agent 就同时拥有它们的工具。

## 四、客户端生命周期

1. **initialize**：建立连接、交换双方信息（名字/版本/能力）。
2. **listTools**：拿到 Server 提供的工具列表（name + description + JSON Schema）。
3. **callTool**：按需调用工具、传参数、拿结果。
4. **close**：断开连接、释放资源。

## 五、本地怎么跑通（stdio）

1. 把我们的 Server 打成可执行 jar（或写个 main）。
2. `StdioMcpTransport.command(...)` 指向该 jar。
3. Agent 发自然语言请求，模型选择调用 MCP 工具，Client 通过 stdio 转发给 Server 执行。

> 说明：Day 3 完成了「接入接线」的代码与验证；真正的跨进程往返留到 Day 4（用 HTTP 传输，在项目内可控地跑通）。

## 六、验证

- `McpToolProviderTest`（1 个）：`McpToolProvider.builder().mcpClients(...).build()` 能正确构造（接线编译 + 构建通过）。

## 七、Day 3 完成标准

- [x] 在 Java 项目里接入 MCP Client（LangChain4j McpClient + McpToolProvider 接线）