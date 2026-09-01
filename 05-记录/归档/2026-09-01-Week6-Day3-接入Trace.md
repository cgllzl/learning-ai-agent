# Week 6 Day 3 归档：接入 Trace（2026-09-01）

> 学习计划：`01-每周学习/Week-06-Evaluation与可观测性/学习目标.md` ｜ 笔记：`02-知识库/可观测性/Day3-接入Trace.md`

## 今天做了什么

1. 新建 `TraceSpan` 与 `AgentTracer`：用栈维护 Span 父子关系。
2. 新建 `TraceableOrderAgentService`：通过 `beforeToolExecution / afterToolExecution` 自动记录工具 Span。
3. 用真实 DeepSeek 验证客服查订单场景的完整链路。

## 验证

- `AgentTracerTest`：子 Span 的 parentSpanId 指向根 Span。
- `TraceLiveTest`（真实 DeepSeek）：输出 `AGENT:chat` 根 Span 与 `TOOL:getOrder` 子 Span。

## 完成标准（Day 3）

- [x] 实现最小 Trace：根 Span + 工具 Span
- [x] 一次 Agent 对话全链路可追踪
- [x] 有学习例子和企业例子，并真实调用大模型

## 下一步（Day 4）

- 记录 Token 用量、成本、延迟指标
