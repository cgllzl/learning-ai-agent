# enterprise-agent

企业级 AI Agent 平台（Enterprise AI Knowledge & Operations Agent）。

- 技术栈：Java 21 / Spring Boot 3.5.16 / Maven / LangChain4j 1.18.1（DeepSeek）
- 当前进度：Day 2 —— `/chat` 接口已可用（真实 DeepSeek 联调通过）
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
```

## 接口

### POST /chat —— 对话

```http
POST /chat
Content-Type: application/json

{
  "systemPrompt": "你是企业智能助手，回答要简洁。",
  "messages": [
    { "role": "user", "content": "你好" }
  ]
}
```

响应：

```json
{ "reply": "你好！我是你的企业智能助手……" }
```

- `role` 支持：`system` / `user` / `assistant`
- 错误：参数不合法 → 400；AI 服务调用失败 → 502，返回 `{"error":"..."}`

## 配置说明

| 配置项 | 位置 | 说明 |
| --- | --- | --- |
| `DEEPSEEK_API_KEY` | `.env`（不入库） | DeepSeek API Key，从 https://platform.deepseek.com 获取 |
| `deepseek.base-url` | `application.yml` | 默认 `https://api.deepseek.com` |
| `deepseek.model` | `application.yml` | 默认 `deepseek-chat` |
| 服务端口 | `application.yml` | 默认 `8080` |

## 目录结构

```text
src/main/java/com/enterprise/agent/
├── EnterpriseAgentApplication.java   主程序
└── chat/                             Chat 模块（Day 2）
    ├── ChatController.java           POST /chat 入口
    ├── ChatService.java              调用 LLM 模型
    ├── ChatConfig.java               OpenAiChatModel Bean
    ├── DeepSeekProperties.java       deepseek 配置项
    ├── ChatRequest.java / ChatResponse.java
    └── ChatExceptionHandler.java     统一错误处理
scripts/run-dev.ps1                   本地启动脚本（读取 .env）
scripts/install-docker.ps1            一键安装 Docker Desktop（需管理员）
```

## 里程碑

- Day 1（2026-08-11）：环境搭建完成，空项目跑通
- Day 2（2026-08-12）：第一个 `/chat` 接口（LLM API / Prompt / 校验）
- Day 3 起：Streaming / Structured Output / Retry（见 `04-项目/Sprints/Sprint-01-Chat.md`）
