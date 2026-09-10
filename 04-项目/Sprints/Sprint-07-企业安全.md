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
- [x] 用户、RAG、Memory、Tool 输出和 Agent 消息统一按不可信内容检查
- [x] Tool 执行前使用 Intent Gate 重验原始目标、当前身份、角色、租户、Scope、审批和预算
- [x] 跨 Agent 消息使用结构化身份、Trace、授权 Scope、签名和防重放
- [x] MCP Tool 固定来源、名称、版本、描述指纹和出站主机
- [x] 可疑 Memory 进入隔离区，不进入后续模型上下文
- [x] 敏感读取后未审批外发、异常 Tool 链和级联调用被阻断并审计

## 技术
RBAC / Tenant Isolation / Intent Gate / Indirect Injection / Tool Trust / Signed Agent Message / Memory Quarantine / Audit / Human Approval

## 知识库映射
- `02-知识库/Agent安全/`

## 完成标准
- [x] 越权调用被拒绝
- [x] 高危操作必须审批
- [x] 日志无明文密钥
- [x] 恶意 RAG 文档和 Tool 输出不能改变原始目标或触发外传
- [x] 权限在计划后被撤销时，执行前实时重验会阻断 Tool
- [x] Tool/MCP 描述、版本或出站目标发生未审核变化时默认拒绝
