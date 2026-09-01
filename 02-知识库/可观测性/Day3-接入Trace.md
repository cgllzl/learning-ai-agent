# Day 3：接入 Trace —— 一次对话全链路可追踪

> Week 6 ｜ 归档：`05-记录/归档/2026-09-01-Week6-Day3-接入Trace.md` ｜ 代码：`com.enterprise.agent.observability.AgentTracer`

## 一、先讲人话：Trace 是什么

Agent 的一次回答，内部其实走了很多步：先理解用户 → 调用工具 → 拿到工具结果 → 再生成答案。如果出了错，只看到一个最终回答，很难判断是哪一步慢了、哪一步错了。

Trace（链路追踪）就是把这一整条执行链记录下来：

```text
span-1  AGENT:chat        2604ms
span-2    TOOL:getOrder      5ms
```

每个节点叫一个 **Span**。`AGENT:chat` 是父 Span，`TOOL:getOrder` 是子 Span。这样一眼就能看出：模型本身花了大头，工具调用只花了 5ms。

类比快递物流：

- 一个包裹从下单到签收是一条 Trace；
- 揽收、运输、派送分别是里面的 Span；
- 如果某个环节特别慢，物流轨迹能帮我们定位。

## 二、用代码实现一个最小 Trace

### 1. 定义一个 Span

```java
public record TraceSpan(
        String spanId,
        String parentSpanId,
        String name,
        long startNanos,
        long durationNanos,
        String status,
        Map<String, String> attributes) {
}
```

解释：

- `spanId` / `parentSpanId`：表达父子关系。
- `durationNanos`：耗时，纳秒记录，展示时转毫秒。
- `attributes`：附加信息，比如输入、工具参数、结果。

### 2. 用栈维护父子关系

```java
public void startSpan(String name, Map<String, String> attributes) {
    ActiveSpan parent = stack.get().peek();
    ActiveSpan span = new ActiveSpan(
            "span-" + counter.incrementAndGet(),
            parent == null ? null : parent.spanId(),
            name,
            System.nanoTime(),
            attributes == null ? Map.of() : attributes);
    stack.get().push(span);
}
```

解释：每个线程维护一个 `Deque` 栈。开始一个 Span 就压栈，结束就弹栈。当前栈顶就是父 Span。这样天然支持嵌套。

## 三、把 Trace 接到 Agent 上

`TraceableOrderAgentService` 做了两件事：

1. 用 `AiServices.beforeToolExecution / afterToolExecution` 记录工具 Span；
2. 在 `chat()` 外面包一层 `AGENT:chat` 根 Span。

```java
AiServices.builder(OrderAssistant.class)
        .chatModel(chatModel)
        .tools(orderTools)
        .beforeToolExecution(before -> tracer.startSpan(
                "TOOL:" + before.request().name(),
                Map.of("arguments", before.request().arguments())))
        .afterToolExecution(after -> tracer.endSpan(
                after.hasFailed() ? "ERROR" : "OK",
                Map.of("result", String.valueOf(after.result()))))
        .build();
```

`beforeToolExecution` 在工具执行前触发，`afterToolExecution` 在执行后触发。这样每个工具调用都被自动包成一个 Span，不用改业务代码。

## 四、学习例子 + 企业例子

### 学习例子：父子 Span 关系

`AgentTracerTest` 手工启动根 Span 和工具 Span，验证子 Span 的 `parentSpanId` 指向根 Span。

### 企业例子：客服查订单链路

`TraceLiveTest` 用真实 DeepSeek 模拟「客服查询订单 O1001」，最后打印出：

```text
span-2 <- span-1 | TOOL:getOrder | OK | 5ms
span-1 <- null | AGENT:chat    | OK | 2604ms
```

这个企业场景直接对应线上排障：**如果一个客服查询突然变慢，Trace 能告诉我们慢在模型生成，还是慢在工具/数据库。**

## 五、如何本地测试

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent

# 1) 不调大模型，验证 Span 父子关系
mvn test -Dtest=AgentTracerTest

# 2) 真实 DeepSeek 联调：整次对话 + 工具调用可追踪
.\scripts\test-live.ps1 -Test TraceLiveTest
```

## 六、Day 3 完成标准

- [x] 实现最小 Trace：根 Span + 工具 Span
- [x] 一次 Agent 对话全链路可追踪
- [x] 有学习例子和企业例子，并真实调用大模型
