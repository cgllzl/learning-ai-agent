# 企业订单 Agent 实现（Week 2）

> 归档：`05-记录/归档/2026-08-16-Week2-ToolCalling-Agent.md` ｜ 代码：`04-项目/enterprise-agent/src/main/java/com/enterprise/agent/agent/`

## 一、什么是 Tool Calling

- 让 LLM 不只是「回答问题」，而是**根据用户意图调用你写的 Java 代码**，把结果带回对话。
- 核心机制（Agent Loop）：
  1. 模型收到用户请求，发现需要外部能力 → 返回「要调用哪个工具 + 参数」（结构化输出）
  2. 框架执行你的 Java 方法，拿到真实结果
  3. 把结果回传给模型 → 模型基于结果继续回答
  4. 重复直到模型认为不需要再调用工具（或达到最大轮数）

## 二、LangChain4j 1.18 实现三件套

### 1. 工具（@Tool 方法 = Agent 的能力）

```java
@Component
public class OrderTools {
    @Tool("根据订单号查询订单信息")
    public String getOrder(@P("订单号，例如 O1001") String orderId) { ... }
}
```

- `@Tool("描述")`：描述越清楚，模型越容易选对工具。
- `@P("参数说明")`：给每个参数写说明，模型才知道填什么。
- 返回值是 String 描述（也可以返回对象，框架自动序列化成 JSON 给模型）。

### 2. Agent 接口（声明对话形式）

```java
public interface OrderAssistant {
    @SystemMessage("你是企业订单助手，可以调用工具查询订单……")
    String chat(@UserMessage String message);
}
```

### 3. 装配（AiServices）

```java
OrderAssistant assistant = AiServices.builder(OrderAssistant.class)
        .chatModel(chatModel)   // 接入 DeepSeek（OpenAI 兼容）
        .tools(orderTools)      // 注册工具集
        .build();
```

AiServices 是 LangChain4j 的「Agent 自动装配器」：它会扫描接口 + 工具，自动处理 Agent Loop（生成 ToolSpecification → 执行 → 回填 → 再问）。

## 三、本项目工具（企业订单 Agent）

| 工具 | 说明 | 业务规则 |
| --- | --- | --- |
| `getOrder` | 查订单（含用户/商品/金额/状态） | 无 |
| `getUser` | 查用户 | 无 |
| `getProduct` | 查商品 | 无 |
| `getLogistics` | 查物流 | 无 |
| `updateOrderStatus` | 改订单状态 | 仅 PENDING 可改；状态枚举校验 |

数据用内存模拟（`MockOrderData`），后续可换真实数据库。

## 四、为什么工具描述和参数说明很重要

- 模型靠 `@Tool` 的描述选择工具：描述模糊 → 选错工具。
- 参数靠 `@P` 说明生成：说明不清晰 → 模型填错参数。
- 规则写在工具内部（如「仅 PENDING 可改」），模型只是「发起」，最终由 Java 代码保证正确性 —— **安全边界在代码，不在模型承诺**。

## 五、测试

- `OrderToolsTest`（8 个）：工具行为与业务规则（查得到/查不到、改状态限制、非法状态）。
- `OrderAgentLiveTest`（3 个，真实 DeepSeek）：自然语言「查订单 O1001」→ 回复里出现「机械键盘/399」（只能来自工具返回，证明真的调了 Java 工具）。

## 六、下一步

- 处理 Tool 报错、超时、工具选择错误的兜底（Week 2 Day 6）
- Week 3：RAG（让 Agent 知道公司内部知识）