# Sprint 7：企业安全

## 目标
Agent 具备企业级权限与安全能力。

## 任务
- [x] RBAC：不同角色可用不同 Tool / 功能
- [x] 租户隔离：数据与记忆按租户隔离
- [x] Tool 权限校验（最小权限）
- [x] Prompt Injection 防护实践
- [x] Secret 管理：密钥集中管理、日志脱敏
- [x] 审计日志：记录每次 Agent 决策与 Tool 调用
- [x] 高危操作（如改订单状态）人工审批流

## 技术
RBAC / Tenant Isolation / Secret / Audit / Human Approval

## 知识库映射
- `02-知识库/Agent安全/`

## 完成标准
- [x] 越权调用被拒绝
- [x] 高危操作必须审批
- [x] 日志无明文密钥
