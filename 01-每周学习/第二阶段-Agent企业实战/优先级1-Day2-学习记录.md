# Priority 1 Day 2：演示登录、受信身份与响应式企业前端

> 日期：2026-10-25
> 状态：已完成

## 完成内容

- [x] 新建 React 19 + TypeScript 6 + Vite 8 前端工程。
- [x] 登录页从后端读取6个演示账号，覆盖两个租户与5种角色。
- [x] 后端签发随机 Bearer Token，并从服务端会话恢复受信 userId、tenantId、roles。
- [x] Spring Security 保护 `/api/**`，提供固定 JSON 401、明确 CORS 来源与注销。
- [x] 客服、主管、知识管理员、审计员和系统管理员显示不同导航入口。
- [x] 完成运营总览、工作台路由预览、统一错误条和后续页面占位。
- [x] 900px/620px 响应式布局通过浏览器检查，390×844 手机视口可用。
- [x] 后端6个专项测试、前端4个测试、生产构建与真实 DeepSeek HTTP 联调通过。
- [x] 全量 Java 239个测试、0失败、0错误、39个 LiveTest 普通环境跳过。
- [x] 当天知识库、必看核心代码、API 契约、CI、日志和归档同步。

## 先看什么

先读 [必看核心代码](优先级1-Day2-必看核心代码.md)，再读 [知识库笔记](../../02-知识库/Agent安全/Phase2-Priority1-Day2-受信身份与企业前端壳.md)。前端入口在 `04-项目/enterprise-agent-web`。

## 验证命令

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent
mvn test "-Dtest=DemoSessionServiceTest,DemoAuthenticationIntegrationTest,WorkbenchRoutingServiceTest,WorkbenchRouteControllerTest"
.\scripts\test-live.ps1 -Test DemoAuthenticatedWorkbenchLiveTest

cd ..\enterprise-agent-web
npm test
npm run build
```

## 当前边界

这是本地演示认证：账号和会话在内存中，没有密码，服务重启即失效。前端菜单不是业务授权。订单、RAG、审批与审计接口仍将在 Day 3～7 使用受信 Principal 建立各自后端规则。

下一步 Day 3：统一 AI 工作台 SSE，展示流式回答、引用、步骤、完成/失败状态和 traceId。
