# Priority 1 Day 3：流式 AI 工作台

> 日期：2026-10-28
> 状态：已完成

## 完成内容

- [x] 受保护的 `POST /api/v1/workbench/query/stream` 返回具名 SSE 事件：`step/citations/delta/done`。
- [x] 仅用后端会话身份选取租户演示资料；缺资料时不调用模型，返回明确提示。
- [x] 接入现有 LangChain4j / DeepSeek 流式模型，事件有 `traceId` 与递增序号；失败只返回固定错误码。
- [x] 修复 SSE 异步分派的二次认证，非法请求在模型服务前得到 401。
- [x] 前端展示增量回答、引用、执行阶段、完成/失败/断线状态；支持停止显示和显式重试。
- [x] 保留 Day 2 路由预判，新增 6 个后端离线测试和前端状态、断线、重复提交测试。
- [x] 真实 DeepSeek LiveTest 通过：客服登录后取得 `tenant-a` 演示来源及多段回答。

## 先看什么

先按 [必看核心代码](优先级1-Day3-必看核心代码.md) 的后端顺序阅读，再看 [知识库笔记](../../02-知识库/LLM应用开发/Phase2-Priority1-Day3-流式工作台.md) 中的语法拆解和本地操作。

## 验证命令

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent
mvn test "-Dtest=WorkbenchStreamServiceTest,WorkbenchStreamControllerTest"
.\scripts\test-live.ps1 -Test WorkbenchStreamLiveTest

cd ..\enterprise-agent-web
npm test -- --configLoader native
npm run build -- --configLoader native
```

## 当前边界

引用是 Day 3 的租户隔离演示样本；正式知识库和文档权限在 Day 5 接入。步骤只展示本日确实运行的请求、资料筛选和模型生成；真实订单 Tool 与审批步骤尚未接入。停止按钮取消浏览器读取，不能保证取消已发出的模型请求。

下一步 Day 4：接入权威订单列表、详情与租户隔离，把只读订单查询接到工作台。
