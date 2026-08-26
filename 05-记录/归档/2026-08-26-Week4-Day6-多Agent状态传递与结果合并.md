# Week 4 Day 6 归档：多 Agent 状态传递与结果合并（2026-08-26）

> 学习计划：`01-每周学习/Week-04-Agent编排与MCP/学习目标.md` ｜ 笔记：`02-知识库/Agent编排/Day6-多Agent状态传递与结果合并.md`

## 今天做了什么

1. 新建 `com.enterprise.agent.multiagent` 包，实现一条两 Agent 流水线：
   - Agent A：复用 `OrderAgentService` 查询订单事实。
   - Agent B：新增 `CustomerReplyService`，接收订单事实并让大模型合并成客服回复。
2. 用 `MultiAgentCoordinatorService` 完成状态传递：`orderFacts` 从 A 的输出流向 B 的输入。
3. 验证：
   - `MultiAgentCoordinatorServiceTest`（Mockito）验证状态确实原样传给 Agent B。
   - `MultiAgentLiveTest`（真实 DeepSeek）联调通过，最终回复带 O1001 和 399。

## 核心收获

- 状态传递的落地方式：第一个 Agent 的输出字符串，作为第二个 Agent 的消息变量传入。
- `@UserMessage` 方法级模板 + `@V` 参数注入：把 Java 参数拼进发给大模型的消息。
- 多 Agent 不一定要并行，串行流水线也是一种常见编排；关键是「前一道工序的半成品传给后一道工序」。

## 完成标准（Day 6）

- [x] 实现多 Agent 状态传递：Agent A 的产出传给 Agent B
- [x] 实现结果合并：Agent B 基于状态生成最终答案
- [x] 有真实调用大模型的联调例子（`MultiAgentLiveTest`）

## 下一步（Day 7）

- Week 4 周总结 + 知识库更新
