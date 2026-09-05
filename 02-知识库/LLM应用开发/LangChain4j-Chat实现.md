# LangChain4j 接入 DeepSeek 实现（Day 2）

> 归档：`05-记录/归档/2026-08-12-Day2-Chat接口.md` ｜ 项目代码：`04-项目/enterprise-agent/src/main/java/com/enterprise/agent/chat/`

## 一句话
用 LangChain4j 的 `OpenAiChatModel`（OpenAI 兼容）对接 DeepSeek，封装成 Spring Boot 的 /chat 接口。

## 关键类

| 类 | 职责 |
| --- | --- |
| `DeepSeekProperties` | `@ConfigurationProperties(prefix="deepseek")`，读取 api-key / base-url / model |
| `ChatConfig` | 创建 `OpenAiChatModel` Bean |
| `ChatService` | 组装 ChatMessage → 调用模型 → 返回文本 |
| `ChatController` | `POST /chat` 入口 + 校验 |
| `ChatExceptionHandler` | 400（参数/角色错误）、502（AI 调用失败） |

## LangChain4j 1.x 调用方式（重要）

```java
ChatResponse response = chatModel.chat(chatMessages);   // 不再是 generate()
AiMessage answer = response.aiMessage();
String reply = answer.text();
```

- 消息角色：`SystemMessage.from(...)` / `UserMessage.from(...)` / `new AiMessage(...)`
- 旧 0.x API `ChatLanguageModel.generate()` 在 1.x 已移除，别再用旧教程的写法

## 请求/响应格式

```json
POST /chat
{ "systemPrompt": "可选", "messages": [{"role": "user", "content": "你好"}] }
→ { "reply": "你好！……" }
```

## 关联
- 知识库：`02-知识库/LLM应用开发/`
- 项目 Sprint：`04-项目/Sprints/Sprint-01-Chat.md`
- 下一步：Day 3 SSE 流式输出（/chat/stream）

## 企业落地案例
- 场景：企业客服中心把 `/chat` 作为统一对话底座，前端把客户消息列表传进来，后端负责 System Prompt、参数校验与统一错误处理。
- 真实联调：`.\scripts\test-live.ps1 -Test OrderAgentLiveTest` 或 Apifox 调用真实 DeepSeek，验证“客户问题 → 自然语言回复”这条主链路。
