# Week 6.5 Day 5：MCP 2026-07-28 变化与第三方接入安全

- 日期：2026-09-16
- Level 1 官方资料：
  - MCP 2026-07-28 规范：https://modelcontextprotocol.io/specification/2026-07-28
  - 版本变化：https://modelcontextprotocol.io/specification/2026-07-28/changelog
  - 架构：https://modelcontextprotocol.io/specification/2026-07-28/architecture
  - Transport：https://modelcontextprotocol.io/specification/2026-07-28/basic/transports
  - Authorization：https://modelcontextprotocol.io/specification/2026-07-28/basic/authorization
  - 安全最佳实践：https://modelcontextprotocol.io/docs/2026-07-28/tutorials/security/security_best_practices
  - MCP 2026-07-28 GA 说明：https://blog.modelcontextprotocol.io/posts/2026-07-28/
  - LangChain4j MCP：https://docs.langchain4j.dev/tutorials/mcp/
- Level 2 官方代码与版本资料：
  - Java MCP SDK Changelog：https://github.com/modelcontextprotocol/java-sdk/blob/main/CHANGELOG.md
  - LangChain4j Releases：https://github.com/langchain4j/langchain4j/releases
  - MCP Registry 信任边界：https://github.com/modelcontextprotocol/registry/blob/main/docs/modelcontextprotocol-io/about.mdx
- 关联项目：Sprint 5 MCP、Sprint 7 企业安全

## 一、回顾：Week 4 已经学会了什么

Week 4 已经完成一条真实链路：

```text
DeepSeek
   ↓ 决定调用 getOrder
LangChain4j AiServices
   ↓
McpToolProvider
   ↓
DefaultMcpClient + StdioMcpTransport
   ↓ JSON-RPC over stdio
OrderMcpServer
   ↓
OrderTools.getOrder
```

这解决了“不同工具如何使用统一插座”的问题。MCP 就像 USB 标准：Agent 不必为每个工具重新发明接线方式。

Day 3 又增加了来源、版本、工具描述指纹和出站主机检查。但当时的检查还没有接到真实 `McpToolProvider`，并且没有固定完整参数 Schema。也就是说，仓库里虽然有“防伪码”，模型看到工具前却还没有真正经过这道门。

今天自然要解决两个问题：

1. MCP 标准已经从 2025 的“建立会话”演进到 2026 的“每次请求自带信息”，旧笔记不能继续当成最新事实；
2. 第三方 MCP Server 不是安装后就能得到全部权限，必须先审核、再最小开放、变化后重新审核。

## 二、先分清三件事：规范、SDK、项目实现

这是今天最重要的判断：

> 最新规范写了某项能力，不代表当前依赖已经实现；SDK 实现了，也不代表项目已经安全启用。

本项目实际版本：

| 层次 | 当前版本 | 实际协议能力 |
| --- | --- | --- |
| MCP 最新规范 | `2026-07-28` | 现代无会话协议 |
| LangChain4j MCP Client | `1.18.1-beta28` | 默认 `2025-11-25` 旧线协议 |
| 官方 Java MCP SDK Server | `2.0.1` | 最新实现到 `2025-11-25` |
| LangChain4j 首个声明支持 2026 的版本 | `1.19.0` | 仅说明 Client 新增 2026 支持，仍需和 Server 一起回归 |

因此本日选择：**不盲目升级**。现有代码继续明确固定 `2025-11-25`，新规范先学懂并建立迁移清单。等 Java Client 与 Server 两端都满足要求，再单独升级和全量回归。

数字也不能望文生义：Java SDK `2.0.1` 的“2.0”是 SDK 版本，不等于 MCP `2026-07-28` 协议版本。

## 三、旧规范到新规范：从酒店入住变成随身证件

2025 旧线像入住酒店：先到前台办理 `initialize`，酒店给出会话信息，后续服务都依赖这次入住状态。

2026 新线像每次办事都随身携带证件和授权：请求落到任意服务实例，服务端只看当前请求就能处理，不必先找到原来办理入住的那台机器。

### 兼容性总表

| 维度 | 2025-06-18 / 2025-11-25 旧线 | 2026-07-28 新线 | 当前项目 |
| --- | --- | --- | --- |
| 生命周期 | `initialize` + `notifications/initialized` | 没有初始化握手 | 支持旧线 |
| 会话 | HTTP 可使用 `Mcp-Session-Id` | 核心无会话 | 仅验证 stdio 旧线 |
| 协议版本与 Client 能力 | 建连时协商一次 | 每个请求放在 `_meta` | 不支持逐请求能力 |
| Server 能力 | initialize 结果返回 | `server/discover` | 不支持 |
| Server 向 Client 索取输入 | 连接内反向 JSON-RPC 请求 | MRTR `input_required` | 不支持 |
| Tool/Resource 列表缓存 | 客户端自管缓存和变更通知 | 结果带 `ttlMs`、`cacheScope` | 只有旧线缓存 |
| HTTP 路由 | 网关通常需要看请求体 | `Mcp-Method`、`Mcp-Name` Header | 不支持新路由头 |
| 变化通知 | GET/SSE、主动通知、资源订阅 | `subscriptions/listen` 按需订阅 | 不支持新订阅方式 |
| 长任务 | 核心请求或早期任务词汇 | Extensions 框架下的 Tasks | 当前无 Tasks API |
| Roots/Sampling/Logging | 连接内能力 | 已进入弃用期，至少保留正式弃用窗口 | 只具备部分旧线 API |
| HTTP+SSE | 旧传输 | 已弃用，迁移到 Streamable HTTP | 项目主链路使用 stdio |

## 四、无会话核心、server/discover 与逐请求能力

2026 请求在 `_meta` 中携带协议版本和 Client 能力。服务端不能因为“上一次请求说支持审批”就假设这一次也支持；它必须按当前请求判断。

`server/discover` 像餐厅门口的当日菜单：Client 可以先询问 Server 支持哪些能力、协议版本和缓存信息。但 Server 返回的 `serverInfo` 只是自报信息，适合展示和调试，**不能当作可信身份认证**。

企业价值：

- 普通负载均衡即可把请求发往任意实例，不需要粘性会话；
- 一次请求的能力和版本更容易审计；
- 网关可以按 Header 路由、限流和授权。

代价是：所有“以前存在会话里”的必要状态，都要进入请求、外部持久化，或者使用正式扩展承载。

## 五、MRTR：工具执行到一半发现还缺东西怎么办

MRTR 是 Multi Round-Trip Requests，多轮往返请求。

例如删除客户资料时，Server 发现还缺少人工确认：

```text
Client → tools/call(deleteCustomer)
Server → resultType=input_required，需要用户确认
Client → 完成确认
Client → 重新发送原 tools/call + inputResponses + 原 requestState
Server → resultType=complete
```

`requestState` 是 Server 给出的不透明状态，Client 应原样带回，不应自行解释或修改。重新执行时服务端必须验证它、设置过期时间，并保证幂等，防止攻击者篡改状态或重复产生副作用。

MRTR 可以承载补参数、确认、审批、Sampling 和 Roots 请求，但当前 Java 依赖没有 `input_required` API，所以本项目只记录迁移设计，不伪造一个同名但不兼容的实现。

## 六、缓存、Header 路由、Tasks 和取消

### 1. 缓存提示

2026 的可缓存结果会携带：

- `ttlMs`：还能信任多久；
- `cacheScope`：缓存能否跨用户共享。

安全重点不是“缓存越久越快”，而是身份、租户、Scope 变化后不能继续使用旧工具清单。本项目对第三方 Server 使用 `cacheToolList(false)`，每次 Agent 调用重新获取清单并做指纹检查。

### 2. Header 路由

Streamable HTTP 使用 `MCP-Protocol-Version`、`Mcp-Method` 和必要时的 `Mcp-Name`。网关不解析 JSON-RPC Body 也能判断这是 `tools/call` 还是 `resources/read`，并据此授权和限流。Server 必须校验 Header 与 Body 一致，不能只相信其中一个。

### 3. Tasks 扩展

Tasks 适合报表生成、批量导入等长耗时工作。Server 可以返回任务句柄，Client 使用 `tasks/get` 查询、`tasks/update` 更新或 `tasks/cancel` 请求取消。

取消是“请求停止”，不是“历史副作用自动回滚”。如果任务在取消到达前已经完成，最终状态仍可能不是 cancelled；有副作用的任务仍需幂等和补偿。

截至本日，Tasks 属于独立扩展并仍在演进，当前 Java 依赖没有相应 API。

## 七、第三方 MCP 为什么不能安装后直接全开

把 MCP Server 接进 Agent，相当于同时引入：

- 一段能在本机或远程运行的程序；
- 一份会影响模型决策的 Tool 描述；
- 一组读取、修改和外发数据的权限；
- 一条新的软件供应链。

即使 Server 出现在官方 Registry，也只说明发布者完成了相应命名空间登记，不代表通过企业安全审计。MCP 核心协议也没有用密码学证明“Tool 描述与真实运行行为永远一致”。

因此企业接入至少经过四道门：

```text
准入：来源、Owner、版本、制品摘要、SBOM、许可证
  ↓
暴露：Tool/Resource/Prompt 清单、描述、完整 Schema、风险、Scope
  ↓
调用：当前用户/租户、最小权限、参数、审批、网络出口、DLP
  ↓
变更：目录或指纹变化即隔离重审，支持审计与紧急禁用
```

### 学习例子：已有 stdio MCP 仍然可用

Day 5 保留并回归 `OrderMcpServerStdioTest`。新规范发布不代表旧 stdio Server 立即失效，也不能因为资料更新就把已经验证的功能误判为“不再能用”。

stdio 的安全含义必须说清：它会在本机启动一个子进程，通常拥有当前 Java 进程的操作系统权限。因此要固定可执行物/镜像摘要、限制文件和网络权限，并保证 stdout 只传 MCP JSON-RPC，日志走 stderr。

### 企业例子：第三方订单/CRM MCP 渐进开放

第一阶段只审核并开放：

```text
getOrder             risk=READ_ONLY   scope=orders:read
```

以下工具即使 Server 突然新增，也不会自动进入模型工具列表：

```text
updateOrder          risk=MUTATING    scope=orders:write  需要本次工具可见性许可
deleteAllOrders      未审核                           默认隐藏
```

如果 `getOrder` 名称不变，但描述或 Schema 新增 `allTenants`、`customerId` 等参数，完整 `ToolSpecification` 指纹会变化，同样进入重新审核。

这里的“可见性许可”发生在模型生成具体调用参数之前，只决定模型本次能不能看到写 Tool，**不等于参数级业务审批**。真正执行写操作时，还要把用户、租户、订单号、目标状态、参数摘要、审批单、有效期和可执行次数重新校验并消费审批。

## 八、OAuth：Token 是专用门票，不是万能通行证

远程 HTTP MCP 采用 OAuth 时：MCP Server 是 Resource Server，MCP Client 是 OAuth Client。

必须守住：

1. Authorization Code + PKCE；
2. 授权和换 Token 时使用 `resource`，把 Token 绑定到目标 MCP；
3. Server 校验 Token audience，不能接受“发给别的系统”的 Token；
4. 每个 Tool 使用最小 Scope，遇到 403 `insufficient_scope` 再做 step-up；
5. 生产使用 HTTPS，精确校验 redirect URI、issuer 和 state；
6. 禁止 Token Passthrough：MCP Server 调下游 API 时必须换用另一枚专用 Token；
7. Token 不进入 Tool 参数、Prompt、审计日志、异常消息或请求 URL。

本项目的 `McpRemoteCredential` 只实现“已经取得 Token 之后”的配置边界：比较可信授权组件提供的 audience/Scope 与目标配置，并对对象文本脱敏。它不解析 JWT 的 `aud`/`scope` Claim，也不包含完整 OAuth 授权服务器流程，不能把它描述成 OAuth 已全部落地。

## 九、代码调用链

本日安全链路：

```text
本地等价 stdio MCP tools/list
        ↓
McpToolTrustPolicy
  ├─ 是否为绑定到审核部署记录的同一个 McpClient 实例
  ├─ Tool 是否经过审核
  ├─ 描述 + 完整 Schema 指纹是否变化
  ├─ Scope 是否足够
  └─ 写/敏感 Tool 是否获准在本次请求中展示
        ↓ 只有通过项
McpToolProvider.filter(...)
        ↓
McpToolOutputGuard 检查真实返回值
        ↓
AiServices / DeepSeek
```

绑定具体 Client 实例能阻止另一个对象仅复制相同 `key` 后混入 ToolProvider，但它仍不是远端 Server 的密码学身份证明。实际制品摘要必须由部署流水线计算并验证；远程服务还需要 TLS/OAuth，必要时使用 mTLS 或供应商签名。

注意：这个过滤器守住的是“通过 Agent 调用”的入口。若其他业务代码直接调用 `mcpClient.executeTool()`，仍必须经过同一策略或额外执行网关，不能把 Client 裸露给任意调用方。

## 十、新语法拆解与用法说明

### 1. `DefaultMcpClient.builder()`

```java
DefaultMcpClient.builder()
        .key("third-party-order-stdio")
        .transport(StdioMcpTransport.builder().command(command).build())
        .protocolVersion("2025-11-25")
        .cacheToolList(false)
        .build();
```

- `key(...)`：Host 侧的稳定 Client 身份，多 Server 时用来区分工具来源；它不是 Server 自报身份。
- `transport(...)`：指定消息如何传输；stdio 会启动 command 对应的子进程。
- `protocolVersion(...)`：显式固定当前双方真实支持的版本，避免把旧 Server 误当 2026 Server。
- `cacheToolList(false)`：不长期相信第三方工具缓存，每次重新拉取后再比较清单。
- `build()`：当前版本会建立 MCP 连接并执行旧线初始化，因此创建后必须在 `finally` 中 `close()`。

常见误用：仅凭 `key` 或 `serverInfo` 判断 Server 可信；它们都不能替代制品、网络和认证绑定。

### 2. `McpToolProvider.filter(BiPredicate)`

```java
.filter((client, specification) -> policy.isAllowed(...))
```

`BiPredicate<A, B>` 是“接收两个参数并返回 boolean 的函数”：

- `client` 表示工具来自哪个 MCP Client；
- `specification` 包含工具名称、描述和参数 Schema；
- `true` 才把工具交给模型；
- `false` 时模型看不到工具，也拿不到对应执行器。

因此 filter 是调用前硬门禁。Listener 只适合观测；当前 LangChain4j 可能捕获 Listener 异常，不能把“抛异常的 Listener”当成安全锁。

`filterFor(subject, access)` 会捕获当前用户和权限快照，因此对应的 ToolProvider 必须按请求创建或安全缓存，不能把某个高权限用户构造出的 Provider 当成全局单例给其他用户复用。

### 3. `ToolSpecification.toJson()` 与 SHA-256

```java
String fingerprint = McpToolTrustVerifier.fingerprint(specification.toJson());
```

`toJson()` 把名称、描述、完整参数 Schema、metadata 和 strict 转成稳定文本，再做 SHA-256。只比较工具名会漏掉“同名换参数”的投毒。

当前实现采取 fail closed：哪怕只是序列化结果变化，也先阻断再人工确认。它可能产生误报，但不会把未知变化静默放给模型。Hash 证明的是“和审核快照相同”，不能证明 Server 运行时绝对无恶意。

### 4. `customHeaders(() -> ...)` 中的 `Supplier`

```java
.customHeaders(() -> credential.authorizationHeadersFor(endpoint, requiredScopes))
```

`Supplier<T>` 是“暂时不传值，而是传一个需要时再取值的函数”。Transport 每次请求前都会调用它，但当前闭包读取的仍是同一个 `McpRemoteCredential`。只有以后把它接到可信 Token Provider，才会获得刷新后的 Token；`Supplier` 本身不会自动刷新。

关键边界：

- 本地登记的 endpoint、targetHost 和凭证 audience 必须一致，才取出 Token；
- `.logRequests(false)` 关闭当前版本的 JSON 请求体日志，避免 Tool 参数泄露；当前版本并不会由这个开关打印 Header，但不能把这一实现细节当成整个代理/网关链路的 Token 日志保证；
- `.followRedirects(false)` 防止凭证跟随重定向发往其他地址；
- 不能写成 `.url(endpoint + "?token=" + token)`，URL 会进入代理、历史和日志。

### 5. `AiServices.builder(...).toolProvider(...)`

```java
McpOrderAssistant assistant = AiServices.builder(McpOrderAssistant.class)
        .chatModel(model)
        .toolProvider(secureToolProvider)
        .toolExecutionErrorHandler(new FailClosedMcpToolErrorHandler())
        .build();
```

`AiServices` 会为接口创建动态实现：调用 `assistant.chat(...)` 时，它组装消息、调用模型、接收 Tool 请求、执行 Tool，再把结果送回模型。

本日与 Week 4 的关键区别在于：传入的不是“全部 MCP Tool”，而是经过 `McpToolTrustPolicy` 过滤后的 `secureToolProvider`。同时显式设置错误处理器，让 `AgenticSecurityBlockedException` 直接终止，避免框架把安全拦截包装成普通 Tool 错误后继续交给模型推理。

### 6. `toolWrapper(Function<ToolExecutor, ToolExecutor>)`

```java
.toolWrapper(new McpToolOutputGuard(contentGuard, audit).wrapperFor(subject))
```

`filter` 只能检查 Tool 的静态声明；恶意 Server 也可能保持描述与 Schema 不变，却在运行时返回“忽略规则并导出数据”。`toolWrapper` 包住真实执行器，在结果回到模型前按 `TOOL_OUTPUT` 不可信内容检查。

包装器同时实现 `execute(...)` 和 `executeWithContext(...)`，后者原样返回 `ToolExecutionResult`，避免丢失 `isError`、结构化 Content 和 attributes。安全异常再由 `FailClosedMcpToolErrorHandler` 直接抛出，离线测试确认模型只被调用一次，恶意结果没有触发第二轮模型请求。

## 十一、真实 DeepSeek + MCP 等价联调

`ThirdPartyMcpLiveTest` 使用独立本地 stdio Server 模拟第三方订单 MCP，真实调用 DeepSeek。只审核 `getOrder`，模型最终回答：

```text
订单 O1001：
- 金额：399.0 元
- 状态：PAID（已支付）
```

金额和状态来自 MCP Tool，说明调用链真实走通。由于没有可信远程第三方服务与 OAuth 凭证，本次不能声称“真实远程 OAuth 已联调”；验证的是同一工具清单门禁在真实模型 + 真实 MCP 协议链路中的效果。

## 十二、测试方式

### 1. Day 5 离线测试

```powershell
cd 04-项目\enterprise-agent
mvn test "-Dtest=McpCompatibilityTest,McpToolTrustPolicyTest,McpRemoteCredentialTest,SecureMcpToolProviderTest"
```

结果：19 个测试通过，覆盖真实依赖版本与初始化协议、新增/删除/变更 Tool、Schema 指纹、受信 Client 实例、最小 Scope、写 Tool 可见性许可、Provider 实际过滤、运行时输出投毒、audience 配置、Token 脱敏和安全 HTTP Transport。

### 2. MCP 与 Agentic 安全回归

```powershell
mvn test "-Dtest=McpCompatibilityTest,McpToolTrustPolicyTest,McpRemoteCredentialTest,SecureMcpToolProviderTest,OrderMcpServerStdioTest,InMemoryMcpClientTest,AgenticSecurityGuardTest,ThirdPartyMcpLiveTest"
```

普通 Maven 环境结果：31 个测试执行，30 个通过；1 个需要 Key 的 LiveTest 按设计跳过，0 失败、0 错误。

### 3. 真实 DeepSeek 联调

```powershell
.\scripts\test-live.ps1 -Test ThirdPartyMcpLiveTest
```

结果：1 个 LiveTest 通过，真实调用 DeepSeek 与本地 stdio MCP。

### 4. CI 离线门禁

```powershell
.\scripts\eval-ci.ps1
```

结果：38 个评估、轨迹与 MCP 治理测试通过，0 失败、0 错误；GitHub Actions 使用同一测试清单。

### 5. 全量回归

```powershell
mvn clean test
```

结果：237 个测试执行，0 失败、0 错误；35 个需要外部条件的 LiveTest 在普通测试中按设计跳过。

## 十三、当前边界

- 当前 LangChain4j Client 与 Java SDK Server 端到端只验证到 `2025-11-25`，尚未实现 `2026-07-28`。
- 本地等价 Server 不是外部第三方 SaaS，也没有证明远程 OAuth 发现、PKCE 和 step-up 已联调。
- Tool 指纹不能证明 Server 运行时代码永远可信；本地登记的制品摘要也不会自行验证实际进程，仍需部署流水线、沙箱、网络出口和运行时权限。
- 工具名级“可见性许可”不是参数级审批；任何写操作都必须在实际 ToolExecutor 中再次鉴权和消费审批。
- `McpToolProvider.filter` 只保护通过 Agent 的工具曝光，直接调用裸 `McpClient` 仍需统一执行网关。
- 当前输出门禁检查 `ToolExecutionResult.resultText()`；未来接入图片、ResourceLink 或自定义 attributes 时，需要按每种 Content 类型继续校验。
- 远程 Transport 目前只做离线构造与配置验证；尚未验证代理、网关、异常链和真实第三方服务是否会记录 Header。
- 当前内存审计和清单适合学习；生产应放入受版本控制的策略仓库和不可篡改审计系统。

## 十四、小结，并过渡到 Day 6

今天完成的不是“把版本号换成最新”，而是建立了一条可解释的迁移和信任链：

```text
看懂最新规范
  ↓
核对当前 Java 真实能力
  ↓
不支持的能力明确列入迁移清单
  ↓
第三方工具先审核、再最小开放
  ↓
目录变化、权限不足或凭证越界时关闭式阻断
```

下一步 Day 6 学习 Skill。衔接很自然：MCP 解决“外部能力如何标准接入”，Skill 解决“完成一类任务时，应该按什么可复用 SOP 组织这些能力”。Skill 可以选择 MCP Tool，但不能绕过今天建立的权限、审批和审计门禁。

## 十五、Day 5 完成标准

- [x] 能说明 2025 旧线与 2026-07-28 无会话核心的差异。
- [x] 能说明当前项目实际支持到 `2025-11-25`，哪些现代能力暂不支持。
- [x] 第三方 MCP 默认不能获得所有 Tool 和全部业务权限。
- [x] 未审核的新工具、描述或 Schema 变化不会进入模型工具列表。
- [x] 本地验证 audience/targetHost/Scope 配置、对象脱敏、关闭内置日志和禁止重定向。
- [ ] 真实远程 OAuth、Token Claim 校验、下游不透传及全链路日志泄露验证。
- [x] 现有 stdio MCP 回归通过。
- [x] 真实 DeepSeek + 本地等价第三方 MCP 只读任务联调通过。
