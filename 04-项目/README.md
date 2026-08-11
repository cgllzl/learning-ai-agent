# Enterprise AI Knowledge & Operations Agent

> 一个从零开始、能写进简历的企业级 AI Agent 平台。
> 技术主线：Java 17+ / Spring Boot 3 / LangChain4j / MCP / 向量数据库 / Docker / CI-CD。

## 项目形态

企业知识问答 + 业务操作 Agent：
- 对话式访问企业内部知识库（RAG）
- 通过 Tool / MCP 操作业务系统（查订单、查用户、查物流、改状态等）
- 带权限、审计、人工审批，可观测、可评估

## 技术栈（第一阶段固定主栈）

| 层 | 选型 |
| --- | --- |
| 语言 / 框架 | Java 17+ / Spring Boot 3 / Maven |
| Agent 框架 | LangChain4j（主线） |
| 向量数据库 | pgvector 或 Qdrant（Week 3 时定） |
| 记忆 | Redis / MySQL |
| MCP | MCP Client（Java） |
| 部署 | Docker / Docker Compose |
| 评估与观测 | 自建评估脚本 + OpenTelemetry |

## Sprint 一览

| Sprint | 名称 | 新增能力 | 主要技术 | 对应知识库 |
| --- | --- | --- | --- | --- |
| Sprint 0 | 环境搭建 | 项目骨架 + Docker | JDK / Spring Boot / Maven / Docker | `部署与工程化` |
| Sprint 1 | Chat | `/chat` + Streaming + Structured Output | LLM API / Prompt / SSE | `LLM应用开发`、`Prompt工程` |
| Sprint 2 | Tool Calling | 业务操作 Agent | Function Calling / @Tool / Agent Loop | `Tool-Calling` |
| Sprint 3 | 企业知识库 | RAG 问答 + 引用 | Embedding / Vector DB / Rerank | `RAG` |
| Sprint 4 | Memory | 多轮记忆 + 长期记忆 | ChatMemory / Redis | `Memory` |
| Sprint 5 | MCP | 外部工具标准接入 | MCP Client / JSON-RPC | `MCP` |
| Sprint 6 | Multi-Agent | Supervisor 分工 | Orchestration / Handoff | `Agent编排` |
| Sprint 7 | 企业安全 | 权限 / 审批 / 审计 | RBAC / Secret / Audit | `Agent安全` |
| Sprint 8 | Evaluation | 自动化评估 | 评估用例 / CI | `Evaluation` |
| Sprint 9 | Observability | 全链路追踪与指标 | Trace / 指标 | `可观测性` |
| Sprint 10 | Production | 部署上线 | Docker / CI/CD | `部署与工程化` |

## 使用方式

每个 Sprint 的详细任务见 `Sprints/Sprint-XX.md`。每完成一个 Sprint：
1. 在 Sprint 文件勾选完成项
2. 在 `05-记录/知识库-项目映射.md` 登记功能 ↔ 知识库文档
3. 在 `05-记录/学习日志.md` 写当天记录
