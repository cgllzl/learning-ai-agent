# Sprint 1：Chat

> 状态：Day 3（2026-08-13）流式输出完成 ｜ 归档：`05-记录/归档/2026-08-13-Day3-流式输出.md`

## 目标
第一个可用的 AI 对话接口。

## 任务
- [x] POST /chat：JSON 输入（消息列表）→ 文本输出（Day 2）
- [x] 支持 System Prompt 配置（Day 2）
- [x] /chat/stream：SSE 流式输出（Day 3，UTF-8 已处理，真实流式联调通过）
- [ ] Structured Output：JSON Schema 约束关键接口输出（Day 4）
- [ ] Retry / Timeout / Fallback + 统一错误处理完善（Day 5）

## 技术
LLM API / Prompt / Streaming / JSON Schema / 错误处理

## 知识库映射
- `02-知识库/LLM应用开发/`：`DeepSeek配置.md`、`LangChain4j-Chat实现.md`、`SSE流式输出.md`
- `02-知识库/Prompt工程/`

## 完成标准
- [x] 对话接口可用（真实 DeepSeek 联调通过）
- [x] 流式可用（真实 DeepSeek 流式联调通过，多分块 + [DONE]）
- [ ] 模型超时/失败时能重试并降级（Day 5）