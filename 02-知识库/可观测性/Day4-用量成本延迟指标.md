# Day 4：记录 Token 用量、成本、延迟指标

> Week 6 ｜ 归档：`05-记录/归档/2026-09-02-Week6-Day4-用量成本延迟指标.md` ｜ 代码：`com.enterprise.agent.observability.UsageMetricsService`

## 一、先讲人话：除了“能用”，还要“花多少、多快”

企业里用大模型，最常被老板问的三个问题是：

1. 这个功能每个月要花多少 API 费用？
2. 平均一次回复要多久？
3. 有没有异常慢、异常贵的情况？

所以除了 Trace，还要记录三类指标：

- **Token 用量**：输入 Token、输出 Token、总 Token。
- **成本**：根据 Token 数量和单价算出的费用。
- **延迟**：一次模型调用花了多少毫秒。

## 二、LangChain4j 从哪里拿到 Token

`ChatResponse` 自带 `tokenUsage()`：

```java
TokenUsage tokenUsage = response.tokenUsage();
int input = tokenUsage.inputTokenCount();
int output = tokenUsage.outputTokenCount();
int total = tokenUsage.totalTokenCount();
```

所以我们不需要自己估算 Token，直接读模型返回的用量即可。

## 三、成本怎么算

先做一个 `CostCalculator`：

```java
public double calculateUsd(int inputTokens, int outputTokens) {
    double inputCost = inputTokens / 1_000_000.0 * inputPricePerMillion;
    double outputCost = outputTokens / 1_000_000.0 * outputPricePerMillion;
    return inputCost + outputCost;
}
```

解释：

- 大模型价格通常按「每 100 万 Token 多少钱」计价。
- `inputTokens / 1_000_000.0` 把 Token 数转成「百万 Token」单位。
- 输入和输出价格不同，所以分开算再加起来。

## 四、统一记录和汇总

```java
public UsageMetrics record(long durationMillis, TokenUsage tokenUsage) {
    int inputTokens = tokenUsage.inputTokenCount();
    int outputTokens = tokenUsage.outputTokenCount();
    int totalTokens = tokenUsage.totalTokenCount();
    double costUsd = costCalculator.calculateUsd(inputTokens, outputTokens);
    // 存入列表，最后可以算总量、平均延迟
}
```

`UsageSummary` 则汇总：

- 总请求数；
- 总输入 / 输出 / 总 Token；
- 总成本；
- 平均延迟。

## 五、学习例子 + 企业例子

### 学习例子：普通聊天

`UsageMetricsLiveTest` 先调用一次普通聊天，看看一次简单回复要多少 Token。

### 企业例子：客服查询订单（会真实调用 getOrder 工具）

简单聊天只能统计「一句话对话」的成本，企业里更典型的是 **Agent 为了回答一个问题，先调用工具查数据，再生成答案**。这个过程中模型会发起多轮请求，Token 用量应该累计计算。

我们用 `UsageAwareOrderAgentService` 来做这件事：

```java
AiServices.builder(OrderAssistant.class)
        .chatModel(chatModel)
        .tools(orderTools)
        .registerListener(new AiServiceResponseReceivedListener() {
            @Override
            public void onEvent(AiServiceResponseReceivedEvent event) {
                TokenUsage usage = event.response().tokenUsage();
                if (usage != null) {
                    accumulatedUsage.updateAndGet(current -> current.add(usage));
                }
            }
        })
        .build();
```

解释：

- `registerListener` 让每次模型响应回来时，我们都能拿到这次的 `tokenUsage`。
- `accumulatedUsage.add(usage)` 把「第一次工具调用」和「最终生成答案」的 Token 加在一起。
- 整次 `chat()` 结束后，再记录一次总延迟和累计 Token。

这样企业例子就从「一句话聊天」升级成了「客服查询订单 O1001 → 模型调用 getOrder 工具 → 返回订单信息」，真实输出：

```text
[企业例子回答] 我已经查询到订单 O1001 的信息：... 金额 399.0 元 ...
[用量汇总] UsageSummary[
  totalRequests=2,
  totalInputTokens=1339,
  totalOutputTokens=160,
  totalTokens=1499,
  totalCostUsd=5.3753E-4,
  averageDurationMillis=2286.0
]
```

这个场景直接对应企业里最关心的问题：**一个带工具调用的客服 Agent 请求，真实 Token 和成本是多少？** 有了累计用量，才能做预算、做容量规划、做异常告警。

## 六、如何本地测试

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent

# 1) 不调大模型，验证成本与汇总逻辑
mvn test -Dtest=CostCalculatorTest,UsageMetricsServiceTest

# 2) 真实 DeepSeek 联调：记录用量、成本、延迟
.\scripts\test-live.ps1 -Test UsageMetricsLiveTest
```

## 七、Day 4 完成标准

- [x] 记录 Token 用量
- [x] 根据 Token 计算成本
- [x] 记录延迟并汇总
- [x] 有学习例子和企业例子，并真实调用大模型
