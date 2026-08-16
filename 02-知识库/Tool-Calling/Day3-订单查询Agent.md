# Day 3：企业订单 Agent（查询订单）

> Week 2 ｜ 代码：`com.enterprise.agent.agent` ｜ 接口：`POST /agent/order`

## 一、Agent 接口声明

```java
public interface OrderAssistant {
    @SystemMessage("你是企业订单助手。你可以调用工具查询订单……")
    String chat(@UserMessage String message);
}
```

- `@SystemMessage`：给 Agent 设定角色与行为边界。
- `@UserMessage`：把用户输入传给模型。

## 二、装配（AiServices 自动处理 Agent Loop）

```java
OrderAssistant assistant = AiServices.builder(OrderAssistant.class)
        .chatModel(chatModel)
        .tools(orderTools)
        .build();
```

## 三、一次完整对话（Agent Loop 实例）

用户：`查询订单 O1001 的信息`

1. 模型收到问题 → 判断需要 `getOrder` → 返回 `tool_call{name: getOrder, args: {orderId: "O1001"}}`
2. LangChain4j 执行 `orderTools.getOrder("O1001")` → 拿到真实订单数据
3. 把结果回填给模型 → 模型基于真实数据组织回答
4. 模型认为已足够 → 输出最终回答

**验证要点**：回复里出现「机械键盘 / 399」——这两个值只可能来自 `getOrder` 的返回值，说明模型**真的调用了 Java 工具**，而不是瞎编（`OrderAgentLiveTest`）。

## 四、端点

```http
POST /agent/order
{ "message": "查询订单 O1001 的信息" }
→ { "reply": "订单 O1001：用户 张三，商品 机械键盘，金额 399.0 元，状态 PAID" }
```