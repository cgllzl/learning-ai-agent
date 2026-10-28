# Priority 1 Day 3：必看核心代码

> 建议阅读时间：40～50 分钟
> 当天主笔记：`02-知识库/LLM应用开发/Phase2-Priority1-Day3-流式工作台.md`
> 验证命令：`mvn test "-Dtest=WorkbenchStreamServiceTest,WorkbenchStreamControllerTest"`；`.\scripts\test-live.ps1 -Test WorkbenchStreamLiveTest`

## 一、今天读完要明白什么

Day 2 已让工作台认出当前用户。Day 3 的问题是：一次模型回答往往要等待，页面怎样实时显示过程，又怎样知道它最后真的完成了？后端把请求拆成 `step → citations → delta → done` 事件。身份来自 Bearer 会话；引用来自按受信租户筛选的演示资料；模型只写正文。断线或模型失败不能伪装成完成。

## 二、按这个顺序精读 7 个入口

| 顺序 | 文件 / 方法 | 为什么必看、处在调用链哪里 | 输入 → 输出 / 关键分支 | 暂时可跳过 |
| --- | --- | --- | --- | --- |
| 1 | [WorkbenchStreamController.query](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/workbench/WorkbenchStreamController.java) | HTTP 入口、身份与 SSE 桥梁 | `DemoPrincipal + message/mode` → `SseEmitter`；断线/超时关闭 | 普通 import |
| 2 | [WorkbenchStreamService.stream](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/workbench/WorkbenchStreamService.java) | Java 编排主链 | mode 分支、来源筛选、模型回调、固定终态 | 普通 getter |
| 3 | [WorkbenchDemoKnowledge.find](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/workbench/WorkbenchDemoKnowledge.java) | 企业数据边界 | 受信 `tenantId + question` → 本租户演示引用或空列表 | 演示文案措辞 |
| 4 | [DemoBearerAuthenticationFilter](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/identity/DemoBearerAuthenticationFilter.java) | 异步分派认证 | 每次分派从 Token 恢复身份；`shouldNotFilterAsyncDispatch=false` | Day 2 已解释的角色转换细节 |
| 5 | [StreamingChatService.stream](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/chat/StreamingChatService.java) | LangChain4j 回调到业务回调的适配 | 消息列表 → partial/complete/error | 旧 `/chat/stream` 入口 |
| 6 | [streamWorkbenchQuery / SseParser](../../04-项目/enterprise-agent-web/src/api/stream.ts) | 浏览器 POST SSE 入口 | Bearer、字节块、事件帧、断线 | fetch 普通参数 |
| 7 | [StreamingWorkbench](../../04-项目/enterprise-agent-web/src/components/StreamingWorkbench.tsx) 和 [applyStreamEvent](../../04-项目/enterprise-agent-web/src/components/workbenchStreamState.ts) | 页面交互与状态 | 发送/停止/重试、序号去重、最终状态 | CSS 与普通 JSX |

时间紧时先看 1～5。测试优先看后端的“有效身份贯通 SSE”和“缺资料零模型调用”，再看前端的“断线不算成功”。

## 三、完整调用链

```text
用户登录客服小林，再点“发送问题”
  → 前端 fetch POST /api/v1/workbench/query/stream，带 Bearer
  → DemoBearerAuthenticationFilter 从服务端会话恢复 tenant-a
  → Spring Security 的 /api/** 规则放行
  → Controller 创建 SseEmitter 和连接开关
  → Service 发 step(接收)
  → DemoKnowledge 只取 tenant-a 的演示规则
  → Service 发 step(筛选)、citations、step(开始生成)
  → StreamingChatService 调 LangChain4j OpenAiStreamingChatModel
  → DeepSeek 的 partial → delta；complete/error → done
  → SseParser 跨网络块拼完整事件
  → 前端按 traceId/sequence 更新回答、引用和终态
```

无 Token：在 Controller 之前被安全链返回 401，模型调用次数为 0。无匹配知识：Java 发“资料中没有相关内容”和完成终态，模型调用次数为 0。断线：页面在没有 `done` 时标为失败；停止：页面标为 stopped，忽略迟到事件。

## 四、核心代码逐段解释

### 4.1 Controller：把 Java 事件变成 SSE

```java
@PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter query(@AuthenticationPrincipal DemoPrincipal principal,
                        @Valid @RequestBody WorkbenchQueryRequest request) {
    SseEmitter emitter = new SseEmitter(60_000L);
    // ...
    emitter.send(SseEmitter.event().name(event.type()).data(event));
}
```

`@AuthenticationPrincipal` 从已经验证的 SecurityContext 取用户；`@RequestBody` 只含问题和模式。`@Valid` 检查非空与最多 2000 字，不接收租户。`SseEmitter` 是异步响应，`send` 可被后续模型回调多次调用。`name` 让 SSE 客户端识别事件种类；`data` 是序列化后的 JSON。连接关闭、超时或写入异常会将 `open` 设为 false，后续回调不再发送。

常见误用：把 `principal` 改为请求体中的 `tenantId`；把异常详情写进 SSE；将 `SseEmitter` 当作一次性 JSON 返回值。

### 4.2 Service：模型与 Java 各负责哪部分

```java
List<WorkbenchCitation> citations = request.mode() == Mode.KNOWLEDGE
        ? knowledge.find(principal, request.message()) : List.of();
if (request.mode() == Mode.KNOWLEDGE && citations.isEmpty()) {
    run.emit("delta", "资料中没有相关内容。", null, null, null);
    run.finish("COMPLETED", null);
    return;
}
```

Java 决定模式、来源和是否调用模型。`List.of()` 是空不可变列表，通用模式没有伪造引用。`StreamRun` 为每个请求生成 UUID `traceId` 和从 1 开始的序号；`AtomicBoolean terminal` 保证重复完成或错误回调只产生一个 `done`。`synchronized` 让并发回调发送事件时序号和输出顺序一致。

模型仅接收受控系统提示词及用户消息，生成解释文字。知识模式把已选来源以 `[1]` 形式放入用户消息，系统提示词要求仅据此回答。`StreamingChatService.stream(...)` 的五个参数依次是系统提示词、消息列表、增量文本回调、完成回调、错误回调。错误统一映射为 `MODEL_UNAVAILABLE`，不给前端原始异常。

常见误用：把模型产出的“已查订单”当成真实 Tool 步骤；把每个 partial 当作最终回答；断线后继续向关闭的响应写入。

### 4.3 Knowledge：先按租户取，再判断相关性

```java
WorkbenchCitation citation = BY_TENANT.get(principal.tenantId());
if (citation == null || !matches(question)) return List.of();
return List.of(citation);
```

实际代码用几个关键词表达 `matches`。这里的 `principal.tenantId()` 由 Day 2 的 Filter 从后端会话恢复，浏览器没有传租户字段。两个租户分别有 7 天与 3 天演示规则；它们是教学用合成数据，不是可用于真实售后决策的正式制度。Day 5 才接文档 ACL 和检索。

### 4.4 Filter：为什么 SSE 有第二次认证

```java
@Override
protected boolean shouldNotFilterAsyncDispatch() {
    return false;
}
```

SSE 首次请求通过认证后，Servlet 异步响应还会再分派一次。安全上下文按分派生命周期清理，第二次也要从 Bearer 恢复。第一轮集成测试曾在已经输出事件后报“响应已提交而授权被拒绝”；这个覆盖方法修复了实际调用链。`false` 的含义是“不跳过异步分派”。不是把一次分派里的过滤器执行两遍。

### 4.5 既有 LangChain4j 服务：回调的来路

`StreamingChatService` 把本项目的 `ChatRequest.Message` 转成 `List<ChatMessage>`，调用 `OpenAiStreamingChatModel.chat(messages, handler)`。`handler.onPartialResponse` 提供增量字符串，`onCompleteResponse(ChatResponse)` 提供结束信号，`onError(Throwable)` 提供失败。`ChatConfig` 中的模型地址是 DeepSeek 的 OpenAI 兼容接口；这个类名不意味着请求发给 OpenAI。完成响应对象本日不再重复读取正文，因为页面已按 partial 累加。

### 4.6 前端：网络字节块不等于 SSE 事件

`streamWorkbenchQuery` 用 `fetch` 发送 POST 和 Bearer，读取 `response.body.getReader()`。`TextDecoder` 的 `{ stream: true }` 保留跨块 UTF-8 字节；`SseParser` 等到空行才解析一整帧。服务端的一个事件可能被拆成两个网络块，两个事件也可能合在一个块。若连接结束时还没收到 `done`，抛 `STREAM_DISCONNECTED`。

`StreamingWorkbench` 用 `AbortController` 停止浏览器接收，`busy` 阻止重复提交，`lastRequest` 保存显式重试输入。`applyStreamEvent` 只接受当前运行中的、相同 trace 且序号递增的事件；`done` 才变 completed 或 failed。停止不保证模型侧立即取消或不计费。

## 五、当天新语法 / API

| 语法 / API | 解决什么问题 | 参数、返回值与时机 | 常见误用 |
| --- | --- | --- | --- |
| `SseEmitter(60_000L)` | 在 Controller 返回后继续写响应 | 毫秒超时；返回异步响应对象 | 超时后标为成功 |
| `SseEmitter.event().name(...).data(...)` | 输出具名 SSE 和 JSON 负载 | 每个事件调用一次 | 一次性拼完整回答才发送 |
| `shouldNotFilterAsyncDispatch() == false` | 异步分派重建身份 | ASYNC 分派时再次执行 Filter | 误以为初次分派执行两遍 |
| `StreamingChatResponseHandler` | 接收 LangChain4j 模型回调 | partial/complete/error，异步返回 | partial 覆盖旧文本 |
| `List<ChatRequest.Message>` | 承载用户消息 | 系统提示词单独传给既有服务 | 把前端身份当系统消息信任 |
| `AtomicBoolean.compareAndSet(false, true)` | 只发送一个终态 | 首次成功返回 true | 重复 done |
| `ReadableStream.getReader()` | 逐块读取 POST SSE 响应 | `read()` 返回 done/value | 每个网络块直接 JSON.parse |
| `TextDecoder.decode(value, {stream:true})` | 跨字节块恢复 UTF-8 | 返回当前可解码文本 | 半个汉字显示乱码 |
| `AbortController` | 停止浏览器请求 | `signal` 给 fetch，`abort()` 中止 | 以为必然取消模型计费 |

新模型调用语法复用 Day 3 之前的 `StreamingChatService`，今天首次在企业工作台调用的参数含义、消息转换、回调与误用见上面 4.2、4.5 和 [当天知识库笔记](../../02-知识库/LLM应用开发/Phase2-Priority1-Day3-流式工作台.md)。

## 六、重点精读 3 个测试

### `validBearerReceivesNamedSseEventsButMissingBearerNeverCallsModel()`

- 测试目的：证明安全链、异步分派和 SSE 输出一起工作。
- 准备条件：完整 Spring 上下文，只有 `StreamingChatService` 被 `@MockitoBean` 替换；先无 Token，再登录取得真实会话 Token。
- 调用链：非法 POST → 401 → `verify(..., never())`；合法 POST → Filter → Controller → Service → Mock 的 partial/complete 回调 → `asyncDispatch`。
- 关键断言：SSE Content-Type、citations/delta/done 名称及 `tenant-a-after-sales`；无 Token 不触达模型。
- 为什么得到结果：服务端会话恢复 Principal，Filter 在 ASYNC 分派也恢复身份，Mock 只代替外部模型不代替安全链。

### `tenantScopedKnowledgeStreamsStepsCitationAndOnlyOneTerminalEvent()`

- 测试目的：确认租户隔离、事件顺序和重复回调防护。
- 准备条件：两个租户 Principal；Mock 模型回传两段文字并故意调用完成两次。
- 调用链：Service → 本租户来源 → step/citations → partial → done。
- 关键断言：序号 1～7 连续、同一 traceId、tenant-a 与 tenant-b 分别只见自己的来源、只有一个 done。
- 为什么得到结果：资料先由受信租户筛选，`AtomicBoolean terminal` 让第二次完成无效。

### `跨网络分块拼出事件，并携带 Bearer 而不提交租户`

- 测试目的：确认前端面对半帧也能正确解析，并保持身份边界。
- 准备条件：模拟 `ReadableStream` 把一个 JSON 事件拆成两个字节块。
- 调用链：`streamWorkbenchQuery → fetch → reader.read → TextDecoder → SseParser → onEvent`。
- 关键断言：收到 `delta/done`；请求只有 `message/mode`，Token 在 Header，没有租户字段。
- 为什么得到结果：解析器等空行才解析，发送体由 API Client 明确构造。

其他测试索引：`missingKnowledgeReturnsHonestAnswerWithoutCallingModel` 证明零模型调用；`explicitOtherTenantQuestionDoesNotAttachCurrentTenantDocument` 证明明确询问其他租户时不误答；`closedClientDoesNotReceiveLateModelCallbacks` 证明迟到片段丢弃；`modelFailureReturnsFixedCodeAndOneFailedTerminalEvent` 证明安全错误码；`StreamingWorkbench.test.tsx` 验证重复提交、停止与重试；`workbenchStreamState.test.ts` 验证序号去重和完成后不再追加；`WorkbenchStreamLiveTest` 真实调用 DeepSeek。

## 七、今天可以先跳过

- `WorkbenchQueryRequest`、`WorkbenchCitation`、`WorkbenchStreamEvent` 的普通 record 字段；只需知道契约。
- `styles.css` 与普通按钮/卡片 JSX；重点是前端状态如何跟事件走。
- Day 2 路由预判的实现细节；它仍保留在工作台下方。
- 旧 `/chat/stream` 的格式；今天产品入口是具名 JSON SSE。

## 八、自检问题

1. 为什么前端收到一个 `delta` 不能认为回答已完成？
2. 为什么 `KNOWLEDGE` 模式要先用后端 Principal 选来源？
3. 没有匹配资料时，为什么应该跳过模型？
4. 为什么 SSE 异步分派还需要 Filter 恢复身份？
5. 浏览器停止后，模型请求是否保证停止、是否保证不计费？
6. 今天显示的 `step` 是哪些真实动作？为什么还不能显示订单 Tool 步骤？
