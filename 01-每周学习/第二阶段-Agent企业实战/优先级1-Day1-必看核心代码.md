# Priority 1 Day 1：必看核心代码

> 建议阅读时间：20～25分钟
> 当天主笔记：`02-知识库/Agent编排/Phase2-Priority1-Day1-企业工作台架构与路由契约.md`
> 验证命令：`.\scripts\test-live.ps1 -Test WorkbenchRoutingLiveTest`

## 一、今天读完要明白什么

今天只做一张“取号单”：模型理解用户要查订单、问制度，还是做高风险操作；Java 把输出变成固定页面建议。它没有查订单、检索文档或执行审批。记住这个边界，就能读懂后续 Day 2～8 为什么还要接身份、Tool、RAG 和审计。

## 二、按这个顺序看核心代码

| 顺序 | 文件 / 方法 | 为什么必看 | 重点 |
| --- | --- | --- | --- |
| 1 | [TicketClassifierAssistant.classify(...)](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/workflow/TicketClassifierAssistant.java) | 认识模型实际收到的分类规则 | `ORDER/KNOWLEDGE/HUMAN`，高风险 → HUMAN |
| 2 | [TicketCategory.fromModelOutput(...)](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/workflow/TicketCategory.java) | 看 Java 怎样收紧模型输出 | 只认精确枚举；含糊文本变 HUMAN |
| 3 | [WorkbenchRoutingService.route(...)](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/workflow/WorkbenchRoutingService.java) | 今天的主流程 | 空输入短路、模型调用、固定页面映射 |
| 4 | [WorkbenchRouteRequest](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/workflow/WorkbenchRouteRequest.java) 与 [WorkbenchRouteResult](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/workflow/WorkbenchRouteResult.java) | 看前后端传什么 | 输入只有 message；返回 `NOT_EXECUTED` |
| 5 | [WorkbenchRouteController.route(...)](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/workflow/WorkbenchRouteController.java) | 看 HTTP 入口 | `POST /api/v1/workbench/route`、`@Valid` |
| 6 | [WorkbenchRoutingServiceTest](../../04-项目/enterprise-agent/src/test/java/com/enterprise/agent/workflow/WorkbenchRoutingServiceTest.java)、[WorkbenchRouteControllerTest](../../04-项目/enterprise-agent/src/test/java/com/enterprise/agent/workflow/WorkbenchRouteControllerTest.java) | 看无模型时如何证明边界 | 固定分类器、HTTP 400、未执行业务 |
| 7 | [WorkbenchRoutingLiveTest](../../04-项目/enterprise-agent/src/test/java/com/enterprise/agent/workflow/WorkbenchRoutingLiveTest.java) | 看真实模型与本地规则如何接在一起 | 三种输入，三种页面，均未执行 |

前5项约15分钟；有余力再看测试。表中链接可直接打开对应文件。

## 三、调用链先在脑中走一遍

```text
用户问题
  → POST /api/v1/workbench/route
  → Controller 用 @Valid 拦空白/过长输入
  → RoutingService 调 classifier.classify(message)
  → LangChain4j AI Service 发给 DeepSeek
  → TicketCategory 严格解析模型字符串
  → Java 映射 WorkbenchView
  → 返回 NOT_EXECUTED
```

输入是 `message`；输出是 `category/suggestedView/requiresHumanReview/executionStatus`。模型只给一个候选类别。空白输入在 HTTP 入口返回 400；模型含糊输出被 Java 收敛到人工队列。

## 四、核心代码逐段理解

### 4.1 为什么 `AiServices.builder(...).build()` 没有执行分类

`build()` 先把接口和模型绑定，得到一个可调用的代理。`classifier.classify(message)` 才把这次问题送往 DeepSeek。泛型目标是 `TicketClassifierAssistant`，它定义 `String classify(String message)`；返回的字符串仍要经过 Java 枚举检查。把构造代理误认为已请求模型，会弄错调用次数与成本。

这类服务有“正式模型构造器”和“测试固定分类器构造器”两个入口，所以正式构造器要加 `@Autowired`。否则局部测试能跑，Spring 整体启动会因不知道选哪个构造器而失败。

### 4.2 为什么 `TicketCategory` 必须严格解析

模型可能回 `ORDER or KNOWLEDGE`，但前端只能打开一个明确区域。Java 的 `switch` 把未知输出归到 `HUMAN`，后续不能拿模糊文本去调用订单 Tool。这个选择是确定性代码，不依赖模型自觉。

### 4.3 为什么响应里有 `NOT_EXECUTED`

目前服务只持有分类器，没有订单 Agent、RAG 或审批执行器。返回 `ORDERS` 的含义是“建议打开订单页”，不是“已经查询 O1001”。`NOT_EXECUTED` 使前端能准确显示“下一步”，不展示虚假的成功状态。

### 4.4 为什么请求里只有 `message`

用户可提交的 JSON 不能证明自己是谁、属于哪个租户、有何权限。因此 Day 1 的请求不接受 `userId/tenantId/scope`。Day 2 的登录与受信上下文会从服务端认证链路建立。

## 五、今天的新语法与常见误用

| 写法 | 作用与返回 | 常见误用 |
| --- | --- | --- |
| `record WorkbenchRouteRequest(...)` | 不可变请求数据，`message()` 读取字段 | 把用户自报租户当受信身份 |
| `@Valid @RequestBody` | JSON 转 Java 对象并执行 Bean Validation | 误认为字段校验等于登录鉴权 |
| `@NotBlank @Size(max=2000)` | 空白和超长问题返回 400 | 只在前端校验，后端不设边界 |
| `AiServices.builder(...).chatModel(...).build()` | 创建 AI Service 代理，不发请求 | 以为 `build()` 已分类 |
| `switch(category)` | 三个枚举分支固定映射页面 | 直接把模型原文当页面 URL |

已有的 `@SystemMessage`、`@UserMessage`、`OpenAiChatModel` 在第一阶段笔记已讲过；今天重点是它们在工作台路由中的位置，详见当天主笔记第五节。

## 六、重点精读三个测试

### `orderAndKnowledgeQuestionsProduceReadOnlyNavigation()`

目的：验证两类正常问题的页面映射。准备：两个固定分类器分别返回 `ORDER`、`KNOWLEDGE`。调用链：`route` → 枚举解析 → 页面映射。关键断言：页面分别是 `ORDERS`、`KNOWLEDGE`，状态都是 `NOT_EXECUTED`。结果原因：服务不持有业务执行器。

### `ambiguousOrBlankInputRoutesToHuman()`

目的：验证含糊输出关闭式转人工。准备：分类器回 `ORDER or KNOWLEDGE`；空输入用“若被调用就抛错”的分类器。调用链：前者严格解析为 `HUMAN`，后者在模型前短路。关键断言：人工队列与未执行状态。结果原因：Java 的白名单拒绝模糊文本。

### `routeApiReturnsContractAndRejectsBlankQuestion()`

目的：验证给前端的实际 HTTP 契约。准备：Mock 服务返回订单建议。调用链：MockMvc POST → JSON 解析/校验 → Controller → 响应序列化。关键断言：四个字段准确，空白或超长问题 HTTP 400 且只有固定错误码 `INVALID_MESSAGE`。结果原因：`@Valid` 在调用服务前拦下无效请求，本地异常处理器给前端稳定且不回显原话的响应。

真实模型测试只要看最后一个方法：DeepSeek 的分类结果经过同一个 Java 路由服务，得到订单、知识和人工三个预期页面。

## 七、今天先跳过什么

- `WorkbenchView` 枚举里的三个常量，读一眼即可。
- Spring Controller 普通构造器、DTO 自动访问方法与测试启动日志。
- 现有 `TicketWorkflowService` 的复核循环细节：今天只复用分类规则，完整业务编排在后续日接入。
- 前端样式：Day 1 尚未创建前端工程。

## 八、自检问题

1. 为什么模型输出 `ORDER or KNOWLEDGE` 最后是 `HUMAN_QUEUE`？
2. `build()` 与 `classifier.classify(...)` 哪一步真正发起模型请求？
3. `suggestedView=ORDERS` 能否说明已经查到订单？为什么？
4. 前端传 `tenantId=tenant-a` 为什么不能直接当作权限依据？
