# Sprint 1：Chat

## 目标
第一个可用的 AI 对话接口。

## 任务
- [ ] POST /chat：JSON 输入（消息列表）→ 文本输出
- [ ] 支持 System Prompt 配置
- [ ] /chat/stream：SSE 流式输出
- [ ] Structured Output：JSON Schema 约束关键接口输出
- [ ] Retry / Timeout / Fallback + 统一错误处理

## 技术
LLM API / Prompt / Streaming / JSON Schema / 错误处理

## 知识库映射
- `02-知识库/LLM应用开发/`
- `02-知识库/Prompt工程/`

## 完成标准
- [ ] 对话接口可用，流式可用
- [ ] 模型超时/失败时能重试并降级
