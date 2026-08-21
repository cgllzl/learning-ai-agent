# enterprise-agent

企业级 AI Agent 平台（Enterprise AI Knowledge & Operations Agent）。

- 技术栈：Java 21 / Spring Boot 3.5.16 / Maven / LangChain4j 1.18.1（DeepSeek）
- 当前进度：Day 3 —— `/chat` 与 `/chat/stream`（SSE 流式）均可用
- 学习配套：知识库根目录 `F:\ChatGPT\学习之路`（本目录即知识库内 `04-项目\enterprise-agent`）

## 环境要求

- JDK 21（本机：`G:\Environment\Java\temurin-21`，需将 `JAVA_HOME` 指向它）
- Maven 3.8+（已装：`G:\Environment\Maven\apache-maven-3.8.4`）

## 快速开始

```powershell
# 1. 配置 DeepSeek API Key
#    复制 .env.example 为 .env，填入 DEEPSEEK_API_KEY

# 2. 启动（自动读取 .env）
.\scripts\run-dev.ps1

# 3. 验证
#    GET  http://localhost:8080/actuator/health  → {"status":"UP"}
#    POST http://localhost:8080/chat             → 对话回复
#    POST http://localhost:8080/chat/stream      → SSE 流式回复
```

## 接口

### POST /chat —— 对话（一次性返回）

```http
POST /chat
Content-Type: application/json

{
  "systemPrompt": "你是企业智能助手，回答要简洁。",
  "messages": [ { "role": "user", "content": "你好" } ]
}
```

响应：`{ "reply": "你好！……" }`

### POST /chat/structured —— 结构化输出（JSON Schema 约束）

```http
POST /chat/structured
Content-Type: application/json

{
  "systemPrompt": "你是信息抽取助手，从用户输入中抽取信息。",
  "messages": [ { "role": "user", "content": "这个产品很好用……" } ],
  "schema": "extract",
  "mode": "json_object"
}
```

响应：`{ "result": { "summary": "...", "keywords": [...], "sentiment": "positive" } }`

- `schema`（内置 5 种场景，默认 `extract`）：
  - `extract` 信息抽取：`{summary, keywords, sentiment}`
  - `ticket` 工单/客服分类：`{category, priority, needs_human, reply}`
  - `classify` 内容分类打标：`{category, tags[], confidence}`
  - `resume` 简历解析：`{name, years, skills[], education[]}`
  - `product` 商品属性抽取：`{name, category, price, stock_status}`
- `mode`：`json_object`（默认，DeepSeek 兼容：JSON 格式约束 + prompt 强化 + 服务端字段校验）；`json_schema`（OpenAI 系原生严格模式，DeepSeek 暂不支持）

### POST /agent/order —— 订单 Agent（Tool Calling）

```http
POST /agent/order
Content-Type: application/json

{ "message": "查询订单 O1001 的信息" }
```

Agent 自动决定调用哪个工具（当前已注册：查订单 / 查用户 / 查商品 / 查物流），返回 `{ "reply": "..." }`。工具数据为内存模拟（`MockOrderData`）。

### POST /rag/ingest —— RAG 文档入库（分块 → Embedding → 向量库）

```http
POST /rag/ingest
Content-Type: application/json

{ "documentId": "DOC1", "content": "Java 21 引入了虚拟线程……" }
```

返回 `{ "documentId": "...", "segmentCount": 2, "segmentIds": [...] }`。本地 Embedding 模型首次运行会下载约 90MB。

### POST /rag/ingest/file —— 文件上传入库（multipart，支持 txt/md）

Apifox：选 multipart/form-data，字段 `file` 选本地 txt/md 文件，可选 `documentId`（不填则用文件名）。单文件上限 10MB。

### POST /rag/search —— RAG 相似度检索（可带元数据过滤）

```http
POST /rag/search
Content-Type: application/json

{ "query": "公司年假有几天？", "documentId": "HR-001", "maxResults": 3 }
```

返回最相似的片段：`{ "chunks": [{ "text": "...", "score": 0.87, "documentId": "HR-001" }] }`

### POST /rag/evaluate —— RAG 评估（召回 + 引用）

先用 `POST /rag/ingest/file` 上传文档，再发：

```http
POST /rag/evaluate
Content-Type: application/json

{ "cases": [ { "question": "年假有几天？", "expectedDocumentId": "HR-001" } ] }
```

返回 `{ total, recallHits, citationHits, recallRate, citationAccuracy, citationPrecision }`。

### POST /rag/hybrid-search —— RAG 混合检索（向量 + 关键词 + RRF）

```http
POST /rag/hybrid-search
Content-Type: application/json

{ "query": "公司年假有几天？", "documentId": "HR-001", "maxResults": 3 }
```

返回融合重排后的片段列表（`chunks`）。

### POST /rag/chat —— RAG 问答（检索 → 生成 + 引用）

```http
POST /rag/chat
Content-Type: application/json

{ "question": "公司年假有几天？", "documentId": "HR-001" }
```

返回 `{ "answer": "根据资料[1]...", "sources": [...] }`。

### POST /chat/stream —— 流式对话（SSE，逐块返回）

```powershell
curl -N -X POST http://localhost:8080/chat/stream `
  -H "Content-Type: application/json" `
  -d '{"messages":[{"role":"user","content":"用三句话介绍 Java 21"}]}'
```

响应（`text/event-stream`）：每个分块一个 `data:` 事件，结束时发送 `data:[DONE]`：

```text
data:Java
data: 21 带来了……
data:[DONE]
```

- `role` 支持：`system` / `user` / `assistant`
- 错误：参数不合法 → 400；AI 服务调用失败 → 502（`/chat`）/ SSE `[ERROR]` 事件（`/chat/stream`）
- 流式接口已显式指定 `charset=UTF-8`，中文不会乱码

## 配置说明

| 配置项 | 位置 | 说明 |
| --- | --- | --- |
| `DEEPSEEK_API_KEY` | `.env`（不入库） | DeepSeek API Key，从 https://platform.deepseek.com 获取 |
| `deepseek.base-url` | `application.yml` | 默认 `https://api.deepseek.com` |
| `deepseek.model` | `application.yml` | 默认 `deepseek-chat` |
| `deepseek.timeout` | `application.yml` | 单次请求超时，默认 `30s` |
| `deepseek.max-retries` | `application.yml` | 失败重试次数，默认 `2`（指数退避） |
| `deepseek.fallback-model` | `application.yml` | 备用模型（重试仍失败时降级），默认同主模型 |
| 服务端口 | `application.yml` | 默认 `8080` |

## 目录结构

```text
src/main/java/com/enterprise/agent/
├── EnterpriseAgentApplication.java   主程序
└── chat/                             Chat 模块
    ├── ChatController.java           POST /chat、/chat/stream 入口
    ├── ChatService.java              一次性对话（调用 LLM）
    ├── StreamingChatService.java     SSE 流式对话（LangChain4j 流式 API）
    ├── ChatConfig.java               OpenAiChatModel / OpenAiStreamingChatModel Bean
    ├── DeepSeekProperties.java       deepseek 配置项
    ├── ChatRequest.java / ChatResponse.java
    └── ChatExceptionHandler.java     统一错误处理
scripts/run-dev.ps1                   本地启动脚本（读取 .env）
scripts/install-docker.ps1            一键安装 Docker Desktop（需管理员）
```

## 容错设计（Day 5）

- **重试**：可重试错误（5xx、429、网络/超时）按指数退避重试，次数由 `max-retries` 控制。
- **超时**：每次请求超时由 `timeout` 配置（作用于模型 Bean）。
- **降级**：主模型重试仍失败时，切到 `fallback-model` 备用模型再试。
- **不可重试错误**（400 参数错误、401 认证失败）不重试、不降级，直接返回错误。
- 全部失败 → HTTP 503 `{"error":"AI 服务不可用…"}`；流式接口保持 `[ERROR]` SSE 事件。
- 核心实现：`ResilientCaller`（`src/main/java/com/enterprise/agent/chat/ResilientCaller.java`）。

## 测试

```powershell
mvn test                        # 单元/接口测试（无需 Key）
mvn test -Dtest=StreamingChatServiceLiveTest   # 真实 DeepSeek 流式联调（需设置 DEEPSEEK_API_KEY）
```

## 里程碑

- Day 1（2026-08-11）：环境搭建完成，空项目跑通
- Day 2（2026-08-12）：`/chat` 对话接口（LLM API / Prompt / 校验）
- Day 3（2026-08-13）：`/chat/stream` SSE 流式输出
- Week 2：Tool Calling 订单 Agent（见 `04-项目/Sprints/Sprint-02-Tool-Calling.md`）