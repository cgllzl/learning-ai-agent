# Week 4 Day 3 归档：接入 MCP Client（2026-08-24）

> 学习计划：`01-每周学习/Week-04-Agent编排与MCP/学习目标.md` ｜ 笔记：`02-知识库/MCP/Day3-接入MCPClient.md`

## 今天做了什么

1. 理清接入链路：AiServices → McpToolProvider → McpClient → Transport → MCP Server。
2. 学习三个组件：McpClient（listTools/callTool）、Transport（stdio/http）、McpToolProvider（桥接到 AiServices）。
3. 编写并验证 `McpToolProvider.builder().mcpClients(...)` 接线；完整 stdio 接入代码记入笔记。

## 验证结果

- `McpToolProviderTest` 通过（接线构建成功）。

## 完成标准（Day 3）

- [x] 接入 MCP Client（LangChain4j）

## 下一步（Day 4）

- 把一个现有 Tool 改造成 MCP Tool，并用 HTTP 传输在项目内跑通真实往返