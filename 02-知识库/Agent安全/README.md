# Agent 安全

## 核心概念
- RBAC（角色权限）
- Tenant Isolation（租户隔离）
- Tool Permission（最小权限）
- Prompt Injection 与防护
- Sensitive Data 保护
- Secret Management（密钥管理）
- Audit Log（审计日志）
- Human Approval（人工审批）

## 常用官方资料
- OWASP Top 10 for LLM Applications：https://owasp.org/www-project-top-10-for-large-language-model-applications/
- MCP 安全相关规范：https://modelcontextprotocol.io

## 本项目实践
- 学习周：Week 5
- 项目：Sprint 7 企业安全

## 笔记列表
- [Day 1：梳理项目里所有 Tool 的权限矩阵（已完成）](Day1-权限矩阵梳理.md)
- [Day 2：给 Agent 接入 RBAC 与租户隔离（已完成）](Day2-RBAC与租户隔离.md)
- [Day 3：Tool 权限校验，模型只能调用有权限的工具（已完成）](Day3-Tool权限校验.md)
