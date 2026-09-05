# Sprint 2：Tool Calling

> 状态：2026-08-18 ｜ ✅ Week 2 全部完成（Day 1~7）｜ 周总结：`01-每周学习/Week-02-Tool-Calling/周总结.md`
## 目标
让 Agent 能调用业务代码，完成「企业订单 Agent」。

## 任务
- [x] 用 @Tool 定义：查订单 / 查用户 / 查物流 / 查商品（4 个工具，真实联调通过）
- [x] 修改订单状态（带参数校验 + 权限校验：仅 PENDING 可改 + 状态枚举校验）
- [x] Agent 自动选择工具，结果回填继续对话
- [x] 最大迭代次数限制，防循环（maxSequentialToolsInvocations）
- [x] Tool 调用错误处理与兜底话术（AiServices 内置兜底 + 工具返回友好错误）

## 技术
Function Calling / @Tool / Agent Loop / JSON Schema

## 知识库映射
- `02-知识库/Tool-Calling/`：`Day1~Day6` 笔记齐全（当前进度：Day 6，待 Day 7 周总结）

## 完成标准
- [x] 自然语言提问能触发正确 Tool（真实联调通过）
- [x] 越权修改订单被拒绝（仅 PENDING 可改）
