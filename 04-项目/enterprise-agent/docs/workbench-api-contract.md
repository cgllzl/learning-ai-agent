# 企业工作台 API 契约（Phase 2 / Priority 1）

> 版本：草案 v0.3（2026-10-28）。本文件区分已实现的认证、路由与演示流式查询，以及后续学习日的正式业务接口。

## 1. 已实现：路由预判

`POST /api/v1/workbench/route`

请求：

```json
{"message":"帮我查询订单 O1001 的金额和状态"}
```

响应：

```json
{
  "category":"ORDER",
  "suggestedView":"ORDERS",
  "requiresHumanReview":false,
  "executionStatus":"NOT_EXECUTED"
}
```

| 字段 | 类型与允许值 | 含义 |
| --- | --- | --- |
| `message` | 非空字符串，最多2000字符 | 用户原话；空值/空白返回 HTTP 400 |
| `category` | `ORDER / KNOWLEDGE / HUMAN` | 模型提出、Java 严格白名单收敛后的类别 |
| `suggestedView` | `ORDERS / KNOWLEDGE / HUMAN_QUEUE` | 前端建议展示的页面 |
| `requiresHumanReview` | Boolean | 是否建议进入人工队列 |
| `executionStatus` | 固定 `NOT_EXECUTED` | 路由阶段没有查订单、检索文档或执行写操作 |

路由规则：只读订单、用户、物流问题 → `ORDER`；制度、流程、文档问题 → `KNOWLEDGE`；取消、退款、改状态、投诉争议、意图含糊或模型输出不符合枚举 → `HUMAN`。模型返回 `ORDER or KNOWLEDGE` 也会转人工。

空白或超过2000字符的 `message` 返回 HTTP 400：`{"error":"INVALID_MESSAGE"}`。错误响应不回显用户原话。

此接口**不是业务授权或执行接口**。它要求有效 Bearer 会话，但不接收用户、租户、Scope 或审批字段；前端不能用路由结果决定是否有权读写订单。它会消耗一次模型调用，后续还要加入配额与追踪。

## 2. Day 2 已实现：演示身份与会话

| 路径 | 是否需要 Token | 用途 |
| --- | --- | --- |
| `GET /api/v1/auth/demo-accounts` | 否 | 返回后端登记的演示账号，不返回令牌 |
| `POST /api/v1/auth/demo-login` | 否 | 输入 `accountId`，签发8小时随机 Bearer Token |
| `GET /api/v1/auth/me` | 是 | 从后端会话返回受信 userId、tenantId、roles |
| `POST /api/v1/auth/logout` | 是 | 撤销当前服务端会话，返回 HTTP 204 |

Token 是随机会话索引，不包含可由前端修改的租户和角色；会话当前保存在内存中，后端重启后失效。缺失、过期、退出后的 Token 或伪造 Token 访问 `/api/**` 返回 HTTP 401：`{"error":"UNAUTHORIZED"}`。该方式用于本地演示，企业生产认证将在后续接入 SSO/OIDC。

## 3. Day 3 已实现：统一工作台流式查询

`POST /api/v1/workbench/query/stream`，需要有效 Bearer Token，`Content-Type: application/json`，响应为 `text/event-stream`。

```json
{"message":"华东零售售后申请期限是什么？","mode":"KNOWLEDGE"}
```

`mode` 只允许 `KNOWLEDGE / GENERAL`；问题非空且最多 2000 字。响应事件依次包含 `step`、`citations`、零个或多个 `delta`、一个 `done`。每个事件的 JSON 数据都有 `type`、`traceId`、递增 `sequence`，并按类型携带 `text`、`citations`、`status`、`errorCode`。`done.status` 是 `COMPLETED / FAILED`。模型失败返回固定 `MODEL_UNAVAILABLE`，不暴露内部异常。无 Token 为 401，非法请求为 400 `INVALID_QUERY`。

`KNOWLEDGE` 当前使用后端内置的两个租户演示规则，先按受信租户选资料；无匹配资料时返回“资料中没有相关内容”，不调用模型。`GENERAL` 不提供订单、审批或写入工具。步骤只代表本日实际执行的接收、资料筛选和模型生成；正式文档检索、可点击原文、订单 Tool 和持久化 Trace 在 Day 4～7 接入。浏览器停止只取消接收，不保证取消已经开始的模型请求。

## 4. 现有学习期接口及接入边界

| 现有路径 | 当前能力 | 工作台接入状态 |
| --- | --- | --- |
| `POST /chat`、`POST /chat/stream` | 通用对话 / 旧 SSE | Day 3 新建受认证的产品接口；旧路径仍作为学习期接口保留 |
| `POST /agent/order` | 学习期订单 Agent | 暂不直接从新 UI 调用；Day 4 先接入可信租户与权限边界 |
| `POST /rag/chat` | 学习期 RAG 问答 | Day 5 接入前补文档权限与引用展示 |
| 工单 `TicketWorkflowService.handle(...)` | 条件分支与有限复核 | 当前为服务层能力；后续统一入口接入 |

## 5. 计划中的产品接口（尚未实现）

| 预计学习日 | 能力 | 候选路径 / 返回重点 |
| --- | --- | --- |
| Day 4 | 订单中心 | `/api/v1/orders`；租户过滤、分页、详情、物流 |
| Day 5 | 知识库中心 | `/api/v1/knowledge/*`；文档权限、索引状态、引用 |
| Day 6 | 审批中心 | `/api/v1/approvals/*`；参数绑定、过期、消费状态 |
| Day 7 | 审计与评估 | `/api/v1/traces/*`、`/api/v1/evaluations/*`；脱敏与角色过滤 |

这些路径在实现日才最终确定。前端不可通过隐藏按钮代替后端授权；任何写操作仍由后端核验用户、租户、参数级审批、幂等键和审计。

## 6. 当前验收

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent
mvn test "-Dtest=WorkbenchRoutingServiceTest,WorkbenchRouteControllerTest,WorkbenchRoutingLiveTest"
.\scripts\test-live.ps1 -Test WorkbenchRoutingLiveTest
.\scripts\test-live.ps1 -Test DemoAuthenticatedWorkbenchLiveTest
mvn test "-Dtest=WorkbenchStreamServiceTest,WorkbenchStreamControllerTest"
.\scripts\test-live.ps1 -Test WorkbenchStreamLiveTest
```

第一条在无 API Key 时运行离线测试并跳过 LiveTest；后两条从 `.env` 加载 Key 后真实调用 DeepSeek，其中 Day 2 LiveTest 经真实 HTTP 登录、Bearer 认证和工作台接口完成路由。
