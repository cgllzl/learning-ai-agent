# 第二阶段 Priority 1 Day 3：从一次性路由到流式 AI 工作台

> 日期：2026-10-28。先读 [必看核心代码](../../01-每周学习/第二阶段-Agent企业实战/优先级1-Day3-必看核心代码.md)，再对照本文运行测试。

## 一、从 Day 2 自然接到 Day 3

Day 2 已解决“谁在调用接口”：浏览器带 Bearer Token，后端恢复受信租户和角色。但路由预判一次返回 JSON，用户看不到模型在生成什么，也无法区分“正在生成、生成完毕、失败、断线”。

Day 3 把一次回答拆成一连串有顺序的小信封。就像快递物流：先收到“已接单”，再收到“找到资料”，随后一段段收到正文，最后必须收到“已送达”或“配送失败”。这就是 SSE（Server-Sent Events）：服务端在一个 HTTP 响应里不断向浏览器发送事件。

今天的企业例子是客服查询“华东零售的售后申请期限”。系统从后端认证会话取得 `tenant-a`，只选择该租户的演示规则，并显示引用 `[1]`。它不会真的退款、改订单或审批。Day 5 再将演示目录替换成带文档权限的正式 RAG。

## 二、请求和响应契约

`POST /api/v1/workbench/query/stream` 需要有效 Bearer Token，请求体只允许业务问题和模式：

```json
{"message":"华东零售的售后申请期限是什么？","mode":"KNOWLEDGE"}
```

`mode` 为 `KNOWLEDGE` 或 `GENERAL`。`KNOWLEDGE` 使用当前租户的演示资料；`GENERAL` 只生成解释，不查询业务记录。用户、租户和角色一律不从请求体读取。

一次成功响应是 `text/event-stream`。每个 `data:` 都是一个 JSON 对象，统一包含 `type`、`traceId`、递增的 `sequence`，其余字段按类型填充：

```text
event:step
data:{"type":"step","traceId":"...","sequence":1,"text":"请求已接收，租户身份来自服务端会话",...}

event:citations
data:{"type":"citations","traceId":"...","sequence":3,"citations":[{"sourceId":"tenant-a-after-sales",...}],...}

event:delta
data:{"type":"delta","traceId":"...","sequence":5,"text":"根据参考资料 [1]",...}

event:done
data:{"type":"done","traceId":"...","sequence":25,"status":"COMPLETED","errorCode":null,...}
```

事件顺序是 `step → step → citations → step → delta* → done`。没有匹配资料时，知识模式返回“资料中没有相关内容”和 `COMPLETED`，不花一次模型调用。模型失败时终态为 `FAILED`，`errorCode=MODEL_UNAVAILABLE`，不把异常堆栈传给浏览器。网络在 `done` 前断开时，前端显示失败，不能把部分回答标为完成。

## 三、调用链：每一步由谁负责

```text
StreamingWorkbench 点击发送
  → streamWorkbenchQuery 用 fetch POST，带 Bearer 和 AbortSignal
  → Spring Security Filter 恢复 DemoPrincipal
  → WorkbenchStreamController 创建 SseEmitter
  → WorkbenchStreamService 生成 traceId 和 step
  → WorkbenchDemoKnowledge 按 principal.tenantId 选演示片段
  → Java 发送 citations
  → StreamingChatService 把消息交给 LangChain4j OpenAiStreamingChatModel
  → DeepSeek 回调 onPartialResponse / onCompleteResponse / onError
  → Java 分别发送 delta / done(COMPLETED) / done(FAILED)
  → 前端 SseParser 拼帧，状态机按 sequence 更新页面
```

这里的 `step` 表示后端确实执行的阶段，不伪造 Tool 调用。今天的模型没有 Tool；Day 4～6 的订单、RAG、审批主链路接入后，才能显示真实 Tool/Workflow 步骤。`traceId` 当前是单次请求的关联 ID，Day 7 再接持久化 Trace 和审计查询。

### 学习小例子：一句话逐段输出

在 `GENERAL` 模式输入“用一句话解释 SSE”。模型逐段返回文字，页面随着 `delta` 追加。停止按钮取消浏览器的 `fetch`；若已送到模型，上游请求可能继续运行并计费，因此页面明确写了这个限制。

### 企业例子：租户售后规则

在 `KNOWLEDGE` 模式问“售后申请期限是什么”。客服小林登录到 `tenant-a`，得到 7 天规则；切换到客服王洁的 `tenant-b`，得到 3 天规则。来源标题注明“演示”，且 `WorkbenchDemoKnowledge.find` 先按后端 Principal 的租户取资料，再匹配问题。若 `tenant-b` 用户明确询问“华东零售”的规则，Java 返回无资料，也不调用模型，避免把本租户规则误答给另一个租户。浏览器不能通过传 `tenantId` 改变来源。

## 四、新语法 / API 拆解

### 4.1 `@PostMapping(... produces = TEXT_EVENT_STREAM_VALUE)` 与 `SseEmitter`

`@PostMapping` 声明一个 POST 接口；`produces` 告诉客户端响应是 SSE，而非普通 JSON。POST 允许发送 JSON 请求体和 Bearer Header；浏览器原生 `EventSource` 只适合 GET，因此前端用 `fetch` 读取 `ReadableStream`。

`new SseEmitter(60_000L)` 创建最长 60 秒的异步响应对象。它不是“60 秒后自动成功”：若超时，连接被关闭，前端缺少 `done` 就显示失败。`SseEmitter.event().name(type).data(record)` 把事件名和 Java record 序列化为 SSE。`emitter.complete()` 正常关闭；写入失败或浏览器断线后，不再向同一响应写入。

常见误用：生成一个 JSON 字符串后一次性返回，却称为“流式”；或者在模型回调里直接把内部异常信息作为事件内容。这里事件名、固定错误码和终态都由 Java 控制。

### 4.2 异步请求为什么要重新执行认证 Filter

普通请求只有初次 Servlet 分派；SSE 使用异步响应，完成时还会有一次 `ASYNC` 分派。Spring Security 在分派结束后清理当前线程的 `SecurityContext`。因此初次分派虽然已认证，第二次分派仍要从同一个 Bearer Token 重新恢复身份。

`DemoBearerAuthenticationFilter.shouldNotFilterAsyncDispatch()` 返回 `false`，意思是“异步分派也运行这个 Filter”。第一次测试确实复现了：若沿用默认跳过行为，SSE 正文已写出后，第二次分派的授权检查抛出 `AuthorizationDeniedException`，测试失败。修复后完整 HTTP 集成测试通过。不要把 `false` 理解为“Filter 在一次分派里执行两遍”；这是两次不同的分派。

### 4.3 LangChain4j 流式回调与消息参数

`StreamingChatService.stream(systemPrompt, List<ChatRequest.Message>, onPartial, onComplete, onError)` 是本项目已有的包装层。它内部先把 `systemPrompt` 和用户消息转换成 `List<ChatMessage>`，然后调用：

```java
streamingChatModel.chat(chatMessages, new StreamingChatResponseHandler() {
    public void onPartialResponse(String partialResponse) { ... }
    public void onCompleteResponse(ChatResponse completeResponse) { ... }
    public void onError(Throwable error) { ... }
});
```

- `OpenAiStreamingChatModel` 是在 `ChatConfig` 中用 DeepSeek 的 OpenAI 兼容地址、Key、模型名和超时构建的 Bean；此处虽然类型名为 OpenAI，实际请求目标是配置的 DeepSeek。
- `ChatMessage` 列表包含系统消息和用户消息。系统消息规定“只能解释、不能声称已执行订单动作”；知识模式另外要求只依据参考资料回答。
- `StreamingChatResponseHandler` 是回调接口。`onPartialResponse` 每次带一段增量文字，并非完整回答；`onCompleteResponse` 是模型正常结束；`onError` 是模型失败。
- `ChatResponse` 在完成回调里可以承载完整输出和元信息；本日只用完成信号，回答由增量片段逐步显示。
- 回调可能晚于 Controller 方法返回，故 `StreamRun` 用同步发送、递增序号和原子终态防止重复 `done`。客户端断开后 `EventSink.isOpen()` 拦住迟到的回调。

适用场景：较长回答、需要实时反馈的工作台。常见误用：把每个 partial 当成完整回答覆盖页面；把模型产出的步骤文字冒充真实 Tool 步骤；以为关闭浏览器连接必然取消模型计费。

### 4.4 `fetch`、`ReadableStream`、`TextDecoder` 与 `AbortController`

`fetch(..., { signal })` 返回 Response，`response.body.getReader()` 按网络字节块读取。一个字节块可能只含半个汉字或半个 SSE 事件，不能对每个块直接 `JSON.parse`。

`TextDecoder.decode(value, { stream: true })` 保留跨块的 UTF-8 字节状态；`SseParser` 再攒到 SSE 空行，抽出 `data:` 解析 JSON。`AbortController.abort()` 让浏览器停止读流；组件使用 `busy` 阻止重复提交，并保存上次问题供显式重试。重试会重新发请求，可能再次产生模型费用。

页面状态机只接受递增的 `sequence`，同一个 trace 的重复事件被忽略；收到 `done` 才能标为完成。`done(FAILED)` 和提前断线都显示失败，用户可以检查错误码和 traceId。

## 五、企业边界与下一步

- Day 3 的资料是按租户隔离的演示样本，尚不是正式文档库、向量检索、文档 ACL 或可点开的原文页面。Day 5 会补这些能力。
- 通用模式没有订单 Tool，模型不能核验订单金额或状态；Day 4 接入权威订单接口后再展示真实数据。
- 停止按钮只取消前端接收；当前 LangChain4j 包装层未提供上游取消句柄。生产环境还需请求配额、并发限制、模型侧取消/超时策略。
- `traceId` 目前只随事件返回，未形成持久化审计。Day 7 衔接 Trace 和 Evaluation。

## 六、如何验证

在后端目录运行离线测试：

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent
mvn test "-Dtest=WorkbenchStreamServiceTest,WorkbenchStreamControllerTest"
```

它验证租户引用、跨租户明确提问被拦截、事件顺序、缺资料零模型调用、迟到回调、固定失败码、401 安全拦截和真实 Spring MVC 异步分派。2026-10-28 实测 6 个测试通过。

真实大模型联调（从本地 `.env` 读取 Key）：

```powershell
.\scripts\test-live.ps1 -Test WorkbenchStreamLiveTest
```

2026-10-28 实测通过：真实 HTTP 登录后调用 DeepSeek，响应包含当前租户引用、多个 `delta`、同一 `traceId` 与 `COMPLETED`，没有其他租户来源。

前端目录运行：

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent-web
npm test -- --configLoader native
npm run build -- --configLoader native
```

Windows 本机中文路径下 Vite 使用原生配置加载方式。界面启动：先在后端目录运行 `.\scripts\run-dev.ps1`，再在前端目录运行 `npm run dev -- --configLoader native`，打开 `http://127.0.0.1:5173/`，登录客服小林并点击“AI 工作台”。

## 七、小结：为什么 Day 4 接订单

今天已经能可信地知道谁在问、把回答逐段送到页面、展示真实的后端步骤和演示引用。下一步用户自然会问“订单 O1001 到底是什么状态”。这不能再由模型猜测，Day 4 要接权威订单数据、租户过滤和只读 Tool，并把真正的查询步骤放进工作台。
