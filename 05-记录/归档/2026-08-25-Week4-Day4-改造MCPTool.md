# Week 4 Day 4 归档：把现有 Tool 改造成 MCP Tool（2026-08-25）

> 学习计划：`01-每周学习/Week-04-Agent编排与MCP/学习目标.md` ｜ 笔记：`02-知识库/MCP/Day4-把现有Tool改造成MCPTool.md`

## 今天做了什么

1. 新建 `OrderMcpServer`：把现有的 `OrderTools.getOrder`（@Tool 写法）翻译成 MCP Tool（描述 + JSON Schema + handler）。
2. 业务逻辑零改动，只换了"对外说明的方式"。
3. 测试：`OrderMcpServerTest` 2 个用例通过（注册成功 + 业务逻辑复用）。

## 核心收获

- @Tool 描述 → Tool.description；@P 参数说明 → JSON Schema；方法本体 → handler；参数从 `request.arguments()` 取。
- 类比：@Tool 是「本店暗号」，MCP Tool 是「标准菜单」——厨师不变，菜单通用。

## 完成标准（Day 4）

- [x] 把一个现有 Tool 改造成 MCP Tool

## 下一步（Day 5）

- 实现 Supervisor 模式：主 Agent 分派任务