# Week 5 Day 2 归档：RBAC 与租户隔离（2026-08-30）

> 学习计划：`01-每周学习/Week-05-企业级Agent安全/学习目标.md` ｜ 笔记：`02-知识库/Agent安全/Day2-RBAC与租户隔离.md`

## 今天做了什么

1. 定义安全主体：`SecuritySubject(userId, tenantId, roles)`。
2. 实现 RBAC：`RbacService` 按角色检查，不通过抛 `AgentAccessDeniedException`。
3. 实现租户上下文：`TenantContext` 用 `ThreadLocal` 绑定当前请求的租户。
4. 实现租户隔离数据：`TenantScopedOrderData`，同一订单号在不同租户下内容不同。
5. 接入 Agent：`SecureOrderAgentService` 先过 RBAC，再绑定租户上下文，最后调用大模型。

## 验证

- `RbacServiceTest`：角色判断正确、无权限抛异常。
- `TenantScopedOrderDataTest`：同号订单按租户隔离、跨租户不可见。
- `SecureOrderLiveTest`（真实 DeepSeek）：t1 客服查 O1001 得到 399；t2 客服查 O1001 得到 1299；EMPLOYEE 角色直接抛异常。

## 完成标准（Day 2）

- [x] 给 Agent 接入 RBAC：按角色判断是否能进入 Agent
- [x] 给 Agent 接入租户隔离：数据按 tenantId 隔离
- [x] 有真实调用大模型的例子（`SecureOrderLiveTest`）

## 下一步（Day 3）

- Tool 权限校验：模型只能调用有权限的工具
