# Week 5 Day 3 归档：Tool 权限校验（2026-08-30）

> 学习计划：`01-每周学习/Week-05-企业级Agent安全/学习目标.md` ｜ 笔记：`02-知识库/Agent安全/Day3-Tool权限校验.md`

## 今天做了什么

1. 实现 `PermissionAwareToolProvider`：实现 LangChain4j 的 `ToolProvider` 接口，按当前用户角色动态过滤工具。
2. 用 `ToolService.findTools(...)` 把 `OrderTools` 的所有 `@Tool` 反射出来，再按 Day 1 的权限矩阵过滤。
3. 接入 `PermissionAwareOrderAgentService`，用 `.toolProvider(...)` 替代原来的 `.tools(...)`。
4. 验证：客服看不到 `updateOrderStatus`，管理员能看到并调用。

## 验证

- `PermissionAwareToolProviderTest`：CUSTOMER_SERVICE 只能看查询工具；ORDER_ADMIN 只能看 updateOrderStatus；未知角色无工具。
- `PermissionAwareOrderLiveTest`（真实 DeepSeek）：客服改不了 O1003，管理员能改成 SHIPPED。

## 完成标准（Day 3）

- [x] 模型只能看到当前用户角色有权使用的工具
- [x] 越权的工具被过滤，实际数据不被越权修改
- [x] 有真实调用大模型的例子（`PermissionAwareOrderLiveTest`）

## 下一步（Day 4）

- Prompt Injection 案例分析 + 防护实践
