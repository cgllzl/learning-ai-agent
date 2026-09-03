# Sprint 9：Observability

## 目标
一次 Agent 对话全链路可追踪、可度量。

## 任务
- [x] 接入 Trace：对话 → 检索 → Tool 调用 → 生成全链路
- [x] Token 用量 / 成本 / 延迟指标
- [x] 日志规范（含敏感信息脱敏）
- [ ] 异常告警

## 技术
Trace / 指标 / 日志 / OpenTelemetry

## 知识库映射
- `02-知识库/可观测性/`

## 完成标准
- [x] 一次对话能在 Trace 中完整还原
- [x] 有成本看板数据
