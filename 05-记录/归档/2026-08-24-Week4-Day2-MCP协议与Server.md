# Week 4 Day 2 归档：MCP 协议概念 + MCP Server（2026-08-24）

> 学习计划：`01-每周学习/Week-04-Agent编排与MCP/学习目标.md` ｜ 笔记：`02-知识库/MCP/Day2-MCP协议与Server.md`

## 今天做了什么

1. 理解 MCP：Client/Server/Tools/Resources/Prompts、JSON-RPC 2.0、生命周期、授权、何时该用/不该用。
2. pom 新增 `langchain4j-mcp`（1.18.1-beta28）与官方 `io.modelcontextprotocol.sdk:mcp`（2.0.1）。
3. 用官方 SDK 写了一个最小 MCP Server（stdio），暴露 `add(a,b)` 工具；单测验证工具注册与逻辑。

## 验证结果

- `SimpleMcpServerTest` 2 个用例通过：listTools 含 add、add(2,3)=5。

## 完成标准（Day 2）

- [x] 跑通一个 MCP Server

## 下一步（Day 3）

- 用 LangChain4j 的 McpClient 接入 Java 项目，真正调用这个 Server 的工具