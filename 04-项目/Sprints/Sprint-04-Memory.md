# Sprint 4：Memory

## 目标
Agent 记住多轮对话和长期信息。

## 任务
- [x] 多轮对话记忆（ChatMemory）
- [x] 上下文管理：超限截断（MessageWindowChatMemory）
- [ ] 长期记忆持久化到 Redis / MySQL
- [x] 记忆与用户/租户隔离

## 技术
ChatMemory / Redis / Context Management

## 知识库映射
- `02-知识库/Memory/`

## 完成标准
- [x] 多轮上下文正确
- [ ] 重启后长期记忆仍在
