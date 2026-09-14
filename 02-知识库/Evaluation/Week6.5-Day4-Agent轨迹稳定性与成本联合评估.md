# Week 6.5 Day 4：Agent 轨迹、稳定性与成本联合评估

- 日期：2026-09-14
- Level 1 官方资料：
  - OpenAI Evaluate agent workflows：https://developers.openai.com/api/docs/guides/agent-evals
  - OpenAI Trace grading：https://developers.openai.com/api/docs/guides/trace-grading
  - LangChain Trajectory Evaluations：https://docs.langchain.com/langsmith/trajectory-evals
- Level 3 论文：
  - AI Agents That Matter：https://arxiv.org/html/2407.01502
  - AgentBench v3：https://arxiv.org/html/2308.03688v3
- 本日结论：只看最终回答像“只听司机说已经安全到达”；轨迹评估要查看行车记录仪，确认他没有闯红灯、绕过审批或开错车。

## 一、回顾：前面已经解决了什么

Week 6 已经实现关键词正确性、引用准确性、失败复盘和 CI 门禁。它能够回答：

> 最终答案里有没有 O1001 和 399？

Day 1～3 又实现了 Workflow、Checkpoint、权限、审批和 Intent Gate。系统现在会真实调用 Tool，也会产生业务副作用。

能力越强，旧评估的盲区越明显。例如 Agent 回答：

> 订单 O1001 金额为 399 元。

文字完全正确，但背后可能发生了三种不同情况：

1. 正确调用 `getOrder(O1001)`；
2. 错误查询 O1002，然后凭记忆猜出 O1001；
3. 查询过程中还误调用了 `updateOrderStatus`。

旧的关键词检查会把三种情况都判为通过。因此，我们自然需要从“批改答案”走向“检查结果和过程”。

## 二、三层评估：回执、账本和行车记录仪

| 层次 | 形象类比 | 检查内容 | 典型失败 |
| --- | --- | --- | --- |
| 最终答案评估 | 看司机交回的回执 | 事实、格式、引用、语气 | 回答缺少金额 |
| 任务结果评估 | 查业务系统账本 | 订单是否真的更新、通知是否真的发送 | Agent 说成功但数据库没变 |
| 执行轨迹评估 | 看全程行车记录仪 | Agent、模型、Tool、参数、审批、Handoff、顺序 | 答案正确但调用写 Tool |

OpenAI、LangChain 等资料通常区分最终响应、单步和完整轨迹。这里把“外部业务状态”单独拆成任务结果层，是针对企业 Agent 有真实副作用所做的工程细化，不是声称所有官方框架都固定使用同一三分法。

本项目最终结果采用硬门禁：

```text
overallPassed = answerPassed
             AND outcomePassed
             AND trajectoryPassed
```

任何一次越权、错误参数或审批绕过都不能被高质量文字平均掉。

## 三、轨迹是什么：一次运行的结构化录像

### 1. `TrajectoryStep`

每一帧记录：

- `sequence`：第几步；
- `agentName`：哪个 Agent；
- `type`：MODEL、TOOL、APPROVAL、HANDOFF 或 AGENT；
- `action`：如 `getOrder`、`approvalGranted`；
- `parameterSummary`：脱敏后的关键参数；
- `status`：OK、ERROR、BLOCKED；
- `durationMillis`；
- 输入/输出 Token；
- 按本次价格表估算的成本。

轨迹不保存隐藏思维过程，也不应保存完整 Prompt、手机号、密钥或原始客户数据。

### 2. `AgentTrajectory`

一条完整轨迹包含 `runId`、`caseId`、最终答案、业务结果、整体耗时和步骤列表，并从步骤自动汇总 Token、成本和操作步骤数。

### 3. 为什么不用旧 Trace 与 Usage 事后拼接

旧 `AgentTracer` 和 `UsageMetricsService` 分别记录调用链与用量，但没有统一的 run 边界、Agent 方案名称和评估用例。Day 4 使用每次运行独立创建的 `TrajectoryRecorder`，在同一次真实调用中记录 Tool、模型响应和 Handoff，避免为了采指标把同一问题重复执行两遍。

## 四、从轨迹到规则：怎样判断这条路线正确

`TrajectoryExpectation` 是机器可执行的路线要求：

- `expectedAnswerFragments`：答案必须包含什么；
- `requiredActions`：必须执行哪些动作；
- `forbiddenActions`：绝不能执行什么；
- `orderedActions`：动作先后关系；
- `requiredArgumentFragments`：关键参数要求；
- `requireTaskCompleted`：业务结果是否必须完成。

### 学习例子：答案正确但走错路

```text
答案：O1001，金额 399 元                   ✅
轨迹：getOrder(O1001)                     ✅
轨迹：updateOrderStatus(O1001, CANCELLED) ❌
```

评估结果：

```text
answerPassed     = true
outcomePassed    = true
trajectoryPassed = false
passed           = false
```

### 企业例子：改单必须先审批

正确路线：

```text
approvalGranted
        ↓
updateOrderStatus(O1003, SHIPPED)
```

如果更新发生在审批前，即使订单最后状态正确，也必须失败。这是“结果正确不代表过程合规”。

## 五、留出集：别让 Agent 背测试答案

开发集像平时练习题，可以反复查看和调 Prompt：

```text
DEV_ORDER_QUERY_O1001
查询订单 O1001 的金额和状态
```

留出集像期末考试，在方案确定前不针对答案调代码：

```text
HOLDOUT_ORDER_QUERY_O1002
帮我看看编号 O1002 的订单现在是什么状态，金额是多少？
```

Day 4 的真实 Single/Multi 对比使用 O1002，而不是前几周反复使用的 O1001。

如果根据留出集失败修改 Prompt，这批题就已经变成开发集，下一轮应更换新的留出集。企业真实留出答案不宜全部提交到公开仓库；仓库可以保存 Schema 和脱敏样例，真实数据放在受控位置。

## 六、为什么一次成功还不够

大模型有随机性。一次通过只能说明“这一次通过”，不能证明稳定。

本日每个方案运行 5 次，计算：

- 成功率：成功次数 / 总次数；
- 成功样本方差：观察成功是否波动；
- 平均 Token 和平均成本；
- 平均延迟与延迟样本标准差；
- P95 延迟；
- 平均步骤数。

### P95 怎么算

将延迟从小到大排序，nearest-rank 位置为：

```text
ceil(0.95 × 样本数)
```

只有 5 个样本时：

```text
ceil(0.95 × 5) = 5
```

所以 P95 就是最大值。它适合学习算法，但不能当作可靠生产 SLO；生产评估通常需要更多样本和更长时间窗口。

## 七、如何公平比较 Single-Agent 和 Multi-Agent

两名选手必须使用相同赛道：

- 相同 DeepSeek 模型和参数；
- 相同 O1002 留出问题；
- 相同订单数据快照；
- 相同 `getOrder` Tool；
- 相同完成标准；
- 相同运行次数；
- 都禁止 `updateOrderStatus`；
- Single 与 Multi 交错运行，减少网络时段差异。

Single-Agent 路线：

```text
OrderAgent → getOrder → 最终回答
```

Multi-Agent 路线：

```text
OrderAgent → getOrder → Handoff → CustomerReplyAgent → 最终回答
```

## 八、为什么使用 Pareto，而不是随便加权总分

把正确率、成本、延迟乘几个任意权重，可能隐藏真实取舍。本项目先排除任何安全或轨迹违规，再做 Pareto 比较。

方案 A 只有在以下指标都不差，并且至少一项更好时，才支配方案 B：

- 成功率不低；
- 平均成本不高；
- P95 延迟不高；
- 平均步骤数不多。

否则返回 `REVIEW_TRADE_OFF`，由业务方决定愿意用多少成本换多少质量。

## 九、真实 DeepSeek 五次对比结果

留出用例：`HOLDOUT_ORDER_QUERY_O1002`。

| 指标 | Single-Agent | Multi-Agent | Multi - Single |
| --- | ---: | ---: | ---: |
| 运行次数 | 5 | 5 | 0 |
| 成功次数 | 5 | 5 | 0 |
| 成功率 | 100% | 100% | 0% |
| 成功样本方差 | 0 | 0 | 0 |
| 平均 Token | 1383.0 | 1557.4 | +174.4 |
| 平均估算成本（USD） | 0.00043068 | 0.00051794 | +0.00008726 |
| 平均延迟 | 1284.2 ms | 2080.6 ms | +796.4 ms |
| P95 延迟 | 1511 ms | 2488 ms | +977 ms |
| 延迟样本标准差 | 176.2 ms | 359.6 ms | +183.4 ms |
| 平均操作步骤 | 3.0 | 5.0 | +2.0 |

两种方案都 5/5 通过，并且都真实调用 `getOrder(O1002)`、没有调用写 Tool。

Multi-Agent 多出一次 Handoff 和一次客服回复模型调用，但本次简单查询没有获得成功率提升。因此结论为：

> 在本次 5 次留出样本下，Single-Agent 在成功率不降低的情况下成本更低、延迟更小、步骤更少，Pareto 支配 Multi-Agent。

这个结论只适用于本次任务和样本，不能推广成“Multi-Agent 永远没用”。复杂任务可能获得质量提升，需要对对应任务重新评估。

### 三种容易混淆的数字

- JUnit 数量：`AgentTrajectoryEvaluationLiveTest` 是 1 个测试方法；
- 业务重复运行：测试方法内部 Single 5 次、Multi 5 次，共 10 条轨迹；
- 模型响应次数：Single 每次约 2 次、Multi 每次约 3 次，本次共记录 25 次模型响应。

它们不是同一个数字。

## 十、测试和 CI

### 1. Day 4 专项离线测试

```powershell
cd 04-项目\enterprise-agent
mvn test "-Dtest=TrajectoryEvaluatorTest,AgentComparisonReportTest,TrajectoryRecorderTest"
```

结果：12 个测试通过，覆盖三层硬门禁、错误 Tool、错误参数、审批顺序、留出集、成功率、方差、P95、Pareto 和轨迹汇总。

### 2. CI 离线门禁

```powershell
.\scripts\eval-ci.ps1
```

结果：19 个测试通过。GitHub Actions 使用同一测试清单，不需要 DeepSeek Key。

### 3. 真实 DeepSeek 重复评估

```powershell
.\scripts\test-live.ps1 -Test AgentTrajectoryEvaluationLiveTest
```

结果：1 个 LiveTest 通过，内部产生 10 条真实轨迹并生成 Single/Multi 对比报告。

### 4. 全量回归

```powershell
mvn clean test
```

最终结果：217 个测试执行，0 失败、0 错误；34 个需要外部条件的 LiveTest 在普通测试中按设计跳过。使用 `clean` 删除了历史遗留的 Surefire XML，避免把已删除测试重复计数。

## 十一、当前实现边界

- 任务结果目前由运行器传入 `taskCompleted`；生产环境应由独立数据库或业务 API 验证，不能听 Agent 自报。
- Tool 参数目前保存截断后的 JSON 字符串；生产 Trace 应按字段白名单解析并脱敏。
- 模型步骤只有 Token，没有单次模型请求耗时；整体 P95 使用顶层运行耗时。
- 评估单价沿用项目现有配置，只用于本批实验比较；应保存价格版本和日期，价格变化后重新计算。
- 5 次样本只能演示方法，不能得出统计显著结论。
- 当前运行器适合串行实验；生产并发场景还需要 run-scoped context 和线程安全存储。

## 十二、本日小结，并自然过渡到 Day 5

今天形成了完整链条：

```text
一次调用留下结构化轨迹
        ↓
三层硬门禁判断是否真正合格
        ↓
同一用例重复运行得到稳定性
        ↓
把质量、成本、延迟和步骤放在一起比较架构
```

我们现在不仅知道 Agent“做得对不对”，还知道它“怎么做、稳不稳、贵不贵”。

下一步 Day 5 学习 MCP 新规范和第三方 MCP 安全接入。原因也很自然：当外部 Tool 数量和来源增加时，今天建立的轨迹、成本和安全评估，正好用来判断第三方 MCP 是否值得信任和接入。

## 十三、Day 4 完成标准

- [x] 区分最终答案、任务结果和执行轨迹评估。
- [x] 轨迹记录 Agent、模型、Tool、参数摘要、状态、Token、成本、耗时和 Handoff。
- [x] 答案正确但行为违规时，三层硬门禁判定失败。
- [x] 实现独立开发集和 O1002 留出集。
- [x] 实现重复运行成功率、样本方差、平均成本、P95 延迟和平均步骤数。
- [x] Single-Agent 与 Multi-Agent 使用相同输入、模型、数据和验收标准。
- [x] 真实 DeepSeek 各运行 5 次并形成数据化结论。
- [x] 轨迹评估接入本地 CI 脚本和 GitHub Actions。
- [x] 12 个专项离线测试和 19 个 CI 门禁测试通过。
- [x] 1 个真实 DeepSeek LiveTest 通过，内部生成 10 条真实轨迹。
