# Priority 1 Day 1：产品架构、页面地图与路由契约

> 日期：2026-10-22
> 状态：已完成

## 今天的目标

把完整企业产品拆成角色、页面、后端模块与可测试接口。今天只落地工作台的“路由预判” API，为 Day 2 前端工程提供第一个真实契约。

## 学习与交付

- [x] 回顾现有 Chat、订单 Agent、RAG 与 Ticket Workflow 的入口和调用关系。
- [x] 确认5种角色与9类页面，画出工作台到后端模块的架构图。
- [x] 确定路由接口请求/响应与失败规则；Day 2 将路径版本化为 `POST /api/v1/workbench/route` 并加认证。
- [x] Java 严格收敛模型输出；含糊或高风险操作建议转人工。
- [x] 离线测试覆盖订单、知识、异常输出与 HTTP 请求校验。
- [x] 真实 DeepSeek 验证订单、制度和取消退款三类输入。
- [x] 当天知识库笔记、必看核心代码指南、项目接口契约和日志同步。

## 阅读顺序

先看 [必看核心代码](优先级1-Day1-必看核心代码.md)，再看 [知识库笔记](../../02-知识库/Agent编排/Phase2-Priority1-Day1-企业工作台架构与路由契约.md)。项目接口原文在 `04-项目/enterprise-agent/docs/workbench-api-contract.md`。

## 验证命令

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent
mvn test "-Dtest=WorkbenchRoutingServiceTest,WorkbenchRouteControllerTest,WorkbenchRoutingLiveTest"
.\scripts\test-live.ps1 -Test WorkbenchRoutingLiveTest
.\scripts\eval-ci.ps1
```

## 结果与边界

离线3个测试通过；真实 DeepSeek 1个 LiveTest 通过，订单 → `ORDERS`，制度 → `KNOWLEDGE`，取消并退款 → `HUMAN_QUEUE`。三个响应的 `executionStatus` 均为 `NOT_EXECUTED`。33 个 Java CI 门禁测试与 5 个 Python Script 测试通过；全量 Java 235 个测试、0 失败、0 错误、38 个需要外部条件的 LiveTest 在普通环境按设计跳过。

全量回归最初发现 Spring 启动失败：服务有两个构造器却未标明注入入口；给正式构造器加 `@Autowired` 后，启动测试与全量回归通过。这验证路由契约，不代表订单读取、知识检索或审批已由新工作台执行。

下一步 Day 2：前端工程、登录与演示身份、角色路由和统一错误状态；后端先建立受信身份契约。
