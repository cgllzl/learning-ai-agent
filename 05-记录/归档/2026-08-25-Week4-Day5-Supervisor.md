# Week 4 Day 5 归档：Supervisor 模式（2026-08-25）

> 学习计划：`01-每周学习/Week-04-Agent编排与MCP/学习目标.md` ｜ 笔记：`02-知识库/Agent编排/Day5-Supervisor模式.md`

## 今天做了什么

1. `SupervisorTools`：把订单子助手（OrderAgentService）、知识子助手（RagQaService）各包装成一个 @Tool。
2. `SupervisorAssistant`：总调度 Agent，@SystemMessage 写清分诊规则。
3. `SupervisorAgentService`：用 AiServices 组装（含防循环上限）。

## 验证结果

- SupervisorToolsTest 2 个用例通过（委托逻辑）。
- SupervisorLiveTest 真实 DeepSeek 通过：问「查询订单 O1001」→ 总机转订单部 → 回复带出 399。

## 完成标准（Day 5）

- [x] 实现 Supervisor 模式：主 Agent 分派任务

## 下一步（Day 6）

- 处理多 Agent 状态传递与结果合并