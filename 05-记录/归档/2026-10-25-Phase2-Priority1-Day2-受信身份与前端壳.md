# Phase 2 Priority 1 Day 2 归档：受信身份与企业前端壳（2026-10-25）

> 笔记：`02-知识库/Agent安全/Phase2-Priority1-Day2-受信身份与企业前端壳.md`
> 必读代码：`01-每周学习/第二阶段-Agent企业实战/优先级1-Day2-必看核心代码.md`

## 完成

- React + TypeScript + Vite 企业前端工程、登录、角色菜单、总览、路由预览与响应式布局。
- Spring Security Bearer 会话：后端目录决定用户、租户和角色；缺失、伪造、过期或注销 Token 返回401。
- 6个演示账号覆盖两个租户、5种角色；会话当前8小时内存保存，服务重启失效。
- `/api/v1/workbench/route` 要求认证；请求体不接受 tenantId 或 roles。
- 本地启动和 LiveTest 使用项目临时目录，绕开损坏的 DJL 默认缓存。

## 验证

- 后端6个专项测试、前端4个测试、TypeScript检查与Vite生产构建通过。
- 真实 HTTP 登录后携带 Bearer Token 调用 DeepSeek 路由通过。
- 浏览器验证客服菜单、工作台实际结果和390×844手机布局。
- 全量 Java 239个测试、0失败、0错误；39个 LiveTest 普通环境按设计跳过。

## 边界

本次是演示身份链，不含密码、持久化、多实例共享和企业 SSO。前端菜单不是授权边界。订单、知识、审批和审计将在后续日用后端 Principal 继续加固。
