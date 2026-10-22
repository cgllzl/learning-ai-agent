# 企业工作台 API 契约（Phase 2 / Priority 1 / Day 1）

> 版本：草案 v0.1（2026-10-22）。本文件区分“已经实现的路由预判”与“后续学习日要实现的业务接口”，避免前端把计划中的接口当成现有能力。

## 1. 已实现：路由预判

`POST /api/workbench/route`

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

此接口**不是权限判断或执行接口**。当前不接收用户、租户、Scope 或审批字段；前端不能用它的结果决定是否有权读写订单。它会消耗一次模型调用，后续应加入身份验证、配额与追踪。

## 2. 现有学习期接口及接入边界

| 现有路径 | 当前能力 | 工作台接入状态 |
| --- | --- | --- |
| `POST /chat`、`POST /chat/stream` | 通用对话 / SSE | Day 3 接入工作台，补请求身份与链路标识 |
| `POST /agent/order` | 学习期订单 Agent | 暂不直接从新 UI 调用；Day 4 先接入可信租户与权限边界 |
| `POST /rag/chat` | 学习期 RAG 问答 | Day 5 接入前补文档权限与引用展示 |
| 工单 `TicketWorkflowService.handle(...)` | 条件分支与有限复核 | 当前为服务层能力；后续统一入口接入 |

## 3. 计划中的产品接口（尚未实现）

| 预计学习日 | 能力 | 候选路径 / 返回重点 |
| --- | --- | --- |
| Day 2 | 登录与会话 | `/api/v1/auth/*`；受信 userId、tenantId、roles |
| Day 3 | AI 工作台统一查询 | `/api/v1/workbench/query` 与 SSE；answer、sources、steps、status、traceId |
| Day 4 | 订单中心 | `/api/v1/orders`；租户过滤、分页、详情、物流 |
| Day 5 | 知识库中心 | `/api/v1/knowledge/*`；文档权限、索引状态、引用 |
| Day 6 | 审批中心 | `/api/v1/approvals/*`；参数绑定、过期、消费状态 |
| Day 7 | 审计与评估 | `/api/v1/traces/*`、`/api/v1/evaluations/*`；脱敏与角色过滤 |

这些路径在实现日才最终确定。前端不可通过隐藏按钮代替后端授权；任何写操作仍由后端核验用户、租户、参数级审批、幂等键和审计。

## 4. Day 1 验收

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent
mvn test "-Dtest=WorkbenchRoutingServiceTest,WorkbenchRouteControllerTest,WorkbenchRoutingLiveTest"
.\scripts\test-live.ps1 -Test WorkbenchRoutingLiveTest
```

第一条在无 API Key 时运行离线测试并跳过 LiveTest；第二条从 `.env` 加载 Key 后真实调用 DeepSeek。
