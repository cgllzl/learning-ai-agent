# Day 2：第一个 Java Tool（@Tool 注解）

> Week 2 ｜ 归档：`05-记录/归档/2026-08-16-Week2-Day2-第一个Java-Tool.md` ｜ 代码：`com.enterprise.agent.agent`

## 一、@Tool 注解：把 Java 方法变成模型可调用的工具

```java
@Component
public class OrderTools {

    @Tool("根据订单号查询订单信息")
    public String getOrder(@P("订单号，例如 O1001") String orderId) {
        // 真正的 Java 代码：查内存/数据库
        return data.findOrderById(orderId)
                .map(order -> "订单 " + order.id() + "：用户 " + order.userId()
                        + "，金额 " + order.amount() + " 元，状态 " + order.status())
                .orElse("未找到订单 " + orderId);
    }
}
```

- `@Tool("描述")`：方法描述，模型据此判断"什么时候调用我"。
- `@P("参数说明")`：参数描述（含示例），模型据此生成参数值。
- 返回值：String 描述（也可以返回对象，框架自动序列化成 JSON 给模型）。
- LangChain4j 会自动把注解翻译成 ToolSpecification（name / description / parameters JSON Schema）。

## 二、注册给 LLM（关键一步）

```java
OrderAssistant assistant = AiServices.builder(OrderAssistant.class)
        .chatModel(chatModel)   // DeepSeek（OpenAI 兼容）
        .tools(orderTools)      // 把工具注册给模型
        .build();
```

- `AiServices.tools(...)` 扫描 `OrderTools` 里的 `@Tool` 方法，注册给模型。
- 注册后，模型在需要时就能"请求调用" `getOrder`，由框架执行并把结果回填。
- 依赖：pom 需要 `langchain4j` 主模块（`AiServices` / `@P` 所在）。

## 三、工具方法三原则

1. **单一职责**：一个方法只做一件事（Day 2 只有 `getOrder`）。
2. **描述清晰**：描述 + 参数示例写得越清楚，模型选对/填对的概率越高。
3. **永不盲信入参**：模型可能给错参数（如不存在的订单号），方法内要做兜底（返回"未找到订单"）。

## 四、验证

- 单元：`OrderToolsTest` —— 直接调用 `getOrder`，断言返回内容与兜底分支。
- 真实联调：`OrderAgentLiveTest` —— 问「查询订单 O1001」，回复里出现「399」（只可能来自工具返回值，证明模型真的调用了 Java 工具）。

## 五、如何观察 Agent 内部请求（logRequests）

在 LiveTest 的模型构造上加两行，就能看到完整的 Agent Loop：

```java
OpenAiChatModel.builder()
        .baseUrl(...).apiKey(...).modelName(...)
        .logRequests(true).logResponses(true)   // 打开请求/响应日志
        .build();
```

实测输出（关键片段）：

1. **第一次请求**（带工具定义）：
```json
"messages": [ {system}, {user: "查询订单 O1001 的信息"} ],
"tools": [ { "type": "function", "function": {
    "name": "getOrder", "description": "根据订单号查询订单信息",
    "parameters": { "properties": { "orderId": {...} }, "required": ["orderId"] } } } ]
```

2. **第一次响应**（模型决定调用工具）：
```json
"message": { "content": "好的，我来为您查询订单 O1001 的信息。",
  "tool_calls": [ { "id": "call_00_...", "type": "function",
    "function": { "name": "getOrder", "arguments": "{\"orderId\": \"O1001\"}" } } ] }
"finish_reason": "tool_calls"
```

3. **第二次请求**（已把 Java 工具结果回填）：
```json
{ "role": "assistant", "content": "好的，我来为您查询订单 O1001 的信息。", "tool_calls": [...] },
{ "role": "tool", "tool_call_id": "call_00_...", "content": "订单 O1001：用户 U1，商品 P1，金额 399.0 元，状态 PAID" }
```

> 证据链：请求 1 带 `tools` → 响应 1 返回 `tool_calls` → 请求 2 里出现 `role: "tool"` 的工具结果（这个结果来自你的 `OrderTools.getOrder`）→ 模型据此给出最终回答。**这就是 AiServices 帮你实现的 Agent Loop。**

## 六、Day 2 范围说明

- 本日只实现：`MockOrderData`（订单）+ `OrderTools.getOrder` + `AiServices` 注册。
- 后续：Day 3 加 Agent 对话接口（HTTP）；Day 4 扩展用户/物流/商品工具；Day 5 修改订单状态。