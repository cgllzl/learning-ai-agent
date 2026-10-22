# Enterprise AI Knowledge & Operations Agent

> 一个从零开始、能写进简历的企业级 AI Agent 平台。
> 技术主线：Java 17+ / Spring Boot 3 / LangChain4j / MCP / 向量数据库 / Docker / CI-CD。

## 项目形态

企业知识问答 + 业务操作 Agent + 可视化运营工作台：
- 对话式访问企业内部知识库（RAG）
- 通过 Tool / MCP 操作业务系统（查订单、查用户、查物流、改状态等）
- 带权限、审计、人工审批，可观测、可评估
- React 企业界面覆盖 AI 对话、订单、知识库、审批、Trace、Evaluation 和治理
- 可生成 smoke/demo/load 三档关联业务数据，用于演示与压测

学习重心保持在 Java 后端：React 界面负责产品体验和端到端展示，租户、权限、审批、幂等、审计与业务正确性全部由后端强制保证。每个学习日另附“必看核心代码”Markdown，帮助从大量改动中优先阅读后端主链路。

## 技术栈（第一阶段固定主栈）

| 层 | 选型 |
| --- | --- |
| 后端 | Java 21 / Spring Boot 3 / Maven |
| 前端 | React / TypeScript / Vite / 企业后台组件库 |
| Agent 框架 | LangChain4j（主线） |
| 数据与检索 | PostgreSQL / pgvector / Redis / 对象存储 |
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
| Sprint 5 | MCP | 外部工具标准接入 + 版本兼容 + 第三方准入 | MCP Client / JSON-RPC / Catalog Diff / Tool Fingerprint / Output Guard | `MCP` |
| Sprint 6 | Multi-Agent / Workflow | Supervisor 分工 + 条件路由 + 有限循环 + 可恢复执行 | Orchestration / Conditional / Checkpoint / Idempotency / Compensation | `Agent编排` |
| Sprint 7 | 企业安全 | 权限 / 审批 / Agentic 安全运行时控制 | RBAC / Intent Gate / Indirect Injection / MCP Trust / OAuth Audience / Structured Delegation / Audit | `Agent安全`、`MCP` |
| Sprint 8 | Evaluation | 结果、轨迹、稳定性与成本联合评估 | Evaluation / Trajectory / Holdout / P95 / Pareto / CI | `Evaluation` |
| Week 6.5 补缺 | Agent Skill | 可复用订单售后 SOP、触发评估与版本治理 | SKILL.md / Progressive Disclosure / Trigger Eval | `Agent-Skill` |
| Sprint 9 | Observability | 全链路追踪与指标 | Trace / 指标 | `可观测性` |
| Sprint 10 | Production | 部署上线 | Docker / CI/CD | `部署与工程化` |
| Sprint 11 | 企业界面与演示数据 | Web 工作台 + 规模数据 + 演示/简历材料 | React / TypeScript / Seed Data / E2E / Load Test | 第二阶段产品定义 |

第二阶段的产品范围、页面、数据规模和简历验收标准见 `01-每周学习/第二阶段-Agent企业实战/项目产品定义与简历验收.md`。

## 使用方式

每个 Sprint 的详细任务见 `Sprints/Sprint-XX.md`。每完成一个 Sprint：
1. 在 Sprint 文件勾选完成项
2. 在 `05-记录/知识库-项目映射.md` 登记功能 ↔ 知识库文档
3. 在 `05-记录/学习日志.md` 写当天记录
