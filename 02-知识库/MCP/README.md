# MCP（Model Context Protocol）

## 核心概念
- MCP 是什么、为什么出现
- Client / Server 架构
- 核心原语：Tools、Resources、Prompts
- JSON-RPC 通信
- 生命周期与授权
- 2025 旧线与 2026 无会话核心的兼容边界
- 第三方 MCP Tool 清单审核、Schema 指纹、最小 Scope 与凭证隔离
- 什么时候该用 MCP、什么时候不该用
- Java 侧接入（LangChain4j MCP）

## 常用官方资料
- MCP 官方文档：https://modelcontextprotocol.io
- MCP 最新正式规范：https://modelcontextprotocol.io/specification/2026-07-28
- MCP 2025-06-18 历史规范：https://modelcontextprotocol.io/specification/2025-06-18
- modelcontextprotocol GitHub：https://github.com/modelcontextprotocol
- LangChain4j MCP：https://docs.langchain4j.dev/tutorials/mcp/

## 本项目实践
- 学习周：Week 4、Week 6.5
- 项目：Sprint 5 MCP Client、Sprint 7 第三方信任策略
- 当前 Java 边界：LangChain4j MCP `1.18.1-beta28` + Java MCP SDK `2.0.1`，端到端使用 `2025-11-25`；`2026-07-28` 现代能力暂未迁移

## 笔记列表
- [Day 2：MCP 协议与 Server（已完成）](Day2-MCP协议与Server.md)
- [Day 3：接入 MCP Client（已完成）](Day3-接入MCPClient.md)
- [Day 4：把现有 Tool 改造成 MCP Tool（已完成）](Day4-把现有Tool改造成MCPTool.md)
- [Week 6.5 Day 5：MCP 2026-07-28 变化与第三方接入安全（已完成）](Week6.5-Day5-MCP-2026-07-28变化与第三方接入安全.md)
- [Week 4 学习总结：Agent 编排 + MCP（已完成）](../Agent编排/Week4-学习总结.md)
