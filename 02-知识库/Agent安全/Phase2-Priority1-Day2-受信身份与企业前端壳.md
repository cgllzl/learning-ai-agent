# Phase 2 · Priority 1 · Day 2：受信身份与企业前端壳

> 日期：2026-10-25
> 学习计划：`01-每周学习/第二阶段-Agent企业实战/优先级1-企业项目主链路整合.md`
> 后端：`com.enterprise.agent.identity`
> 前端：`04-项目/enterprise-agent-web`

## 一、回顾 Day 1：已经会分流，为什么还不能直接办业务

Day 1 做了一张“取号单”：模型把问题分成订单、知识或人工，Java 再把结果固定成页面建议。但取号单没有证明拿号的人是谁。如果前端请求写着 `tenantId=tenant-a`、`role=SYSTEM_ADMIN`，后端直接相信，就像访客自己写一张“我是总经理”的纸条贴在胸前。

Day 2 解决身份来源：前端只选择后端登记的演示账号；后端签发随机会话令牌。以后请求只携带令牌，`userId / tenantId / roles` 都从后端会话恢复。

## 二、通俗理解：会话 Token 像寄存柜手环

活动入场时，工作人员核对名单并发手环。手环上的随机编号只指向后台登记信息，不能由游客修改成“VIP”。每次进入区域时，门禁根据编号在后台查到真实姓名、所属组织和角色。

本次后端调用链：

```text
登录页 GET /api/v1/auth/demo-accounts
  → 用户选择 service-a
  → POST /api/v1/auth/demo-login { accountId }
  → DemoSessionService 从后端账号目录取 DemoPrincipal
  → SecureRandom 生成随机 Token，服务端保存 Token → Principal / expiresAt
  → 前端只在 sessionStorage 保存会话

后续 POST /api/v1/workbench/route
  → Authorization: Bearer <token>
  → DemoBearerAuthenticationFilter 查询后端会话
  → SecurityContextHolder 保存 Authentication
  → SecurityFilterChain 确认 /api/** 已认证
  → @AuthenticationPrincipal 把 DemoPrincipal 交给 Controller
  → DeepSeek 路由，Java 收敛页面建议
```

请求体只有 `message`，没有租户或角色。即便攻击者自己拼 `tenantId`，当前路由 DTO 也不会读取它；后续订单和知识查询必须使用 `DemoPrincipal.tenantId()` 构造查询条件。

## 三、后端设计：谁负责什么

| 组件 | 职责 | 明确不负责 |
| --- | --- | --- |
| `DemoSessionService` | 后端账号目录、随机 Token、过期和注销 | 生产用户库、密码、SSO |
| `DemoBearerAuthenticationFilter` | 从 Header 取 Token，恢复 Principal 并放入 SecurityContext | 决定具体业务权限 |
| `DemoSecurityConfig` | 公开登录接口、保护 `/api/**`、CORS 和 JSON 401 | 订单、知识、审批规则 |
| `DemoAuthController` | 账号列表、登录、`/me`、退出 | 接受前端自报角色或租户 |
| `WorkbenchRouteController` | 从 `@AuthenticationPrincipal` 获取受信身份并调用路由 | 根据隐藏按钮代替后端授权 |

前端菜单过滤只是体验：客服不显示审批页，审计员不显示订单操作页。真正业务权限仍要在后端接口、Service 和数据库查询处验证。Day 2 先建立认证；Day 4～7 接入具体 RBAC 和租户数据条件。

## 四、Spring Security 新语法与调用时机

### 4.1 `OncePerRequestFilter`

`DemoBearerAuthenticationFilter extends OncePerRequestFilter` 表示每个 HTTP 请求最多执行一次过滤逻辑。它读取 `Authorization`，只接受 `Bearer ` 前缀，并调用 `sessionService.resolve(token)`。令牌无效时不制造匿名身份，后面的安全链会返回 401。

常见误用：解析失败仍创建一个只有前端字段的 Principal；这会把“不知道是谁”伪装成“低权限用户”，让审计含糊。

### 4.2 `SecurityContextHolder` 与 `Authentication`

过滤器创建 `UsernamePasswordAuthenticationToken.authenticated(principal, token, authorities)`，然后放入 `SecurityContextHolder`。这里：

- `principal` 是后端 `DemoPrincipal`；
- `credentials` 当前保存随机 Token，生产实现应避免长期保留可重放凭证；
- `authorities` 由后端角色转换成 `ROLE_CUSTOMER_SERVICE` 等权限字符串。

Spring Security 官方把 `Authentication` 定义为当前已认证用户的载体，包含 principal、credentials 和 authorities。[Servlet Authentication Architecture](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html)

### 4.3 `SecurityFilterChain`

`SecurityFilterChain` 决定哪些 Filter 处理当前请求及 URL 规则。Day 2 规则是：演示账号列表与登录公开；`/api/**` 必须认证；第一阶段遗留的 `/chat`、`/agent`、`/rag` 暂时维持原行为，后续逐步迁入产品 API。[Spring Security Filter Architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html)

配置使用 `SessionCreationPolicy.STATELESS`，表示不用 Servlet `HttpSession` 保存登录状态。这里的“会话”由 `DemoSessionService` 自己管理，Token 是索引。401 返回固定 JSON `{"error":"UNAUTHORIZED"}`，方便前端统一处理。

### 4.4 `@AuthenticationPrincipal`

工作台 Controller 方法增加 `@AuthenticationPrincipal DemoPrincipal principal`。参数由 SecurityContext 注入，不来自 JSON。它现在只证明接口已连接受信身份；Day 4 查询订单时会真正使用 `principal.tenantId()`。

### 4.5 `@RequestAttribute` 为什么需要字面量常量

退出接口用 `@RequestAttribute(DemoBearerAuthenticationFilter.ACCESS_TOKEN_ATTRIBUTE)` 获取 Filter 验证过的 Token。注解属性必须是编译期常量；`SomeClass.class.getName() + ".token"` 虽是 `static final`，仍包含方法调用，编译器不接受。本次把常量改成明确字符串，这是首次编译时发现并修正的语法点。

### 4.6 CORS：`localhost` 和 `127.0.0.1` 是两个来源

浏览器来源由协议、主机和端口共同决定。`http://localhost:5173` 与 `http://127.0.0.1:5173` 不相同。第一次浏览器登录失败，就是后端只允许前者，而预览实际使用后者。现在通过 `FRONTEND_ORIGINS` 配置明确列表，并用集成测试检查 `Access-Control-Allow-Origin`。没有使用 `*`。[Spring Security CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)

## 五、会话实现语法

### `SecureRandom` + URL-safe Base64

`SecureRandom` 生成32字节随机数，再用 `Base64.getUrlEncoder().withoutPadding()` 转成适合 Header 的 Token。Token 本身没有 userId、tenantId 和角色，减少前端篡改字段的机会。

### `ConcurrentHashMap`

Web 请求可能并发读写会话，`ConcurrentHashMap<String, StoredSession>` 提供线程安全的基础操作。它仍是单进程内存实现，服务重启会丢失，多个实例也不能共享；Priority 2 会迁移 Redis 或数据库。

### `Clock / Instant / Duration`

生产代码用 `Clock.systemUTC()` 得到当前时间；测试传入可推进的 Clock，不需要真的等待8小时。`Instant` 表示绝对时间点，`Duration` 表示8小时长度。这样过期测试快速且稳定。

## 六、前端只学四件与后端有关的事

### React 状态与副作用

`useState` 保存账号目录、会话、加载和错误状态；`useEffect` 在页面首次打开时读取后端账号目录，并用 `/me` 验证标签页中的旧 Token。React 官方从零构建指引支持用 Vite 建立 React + TypeScript SPA。[React + Vite](https://react.dev/learn/build-a-react-app-from-scratch)

### TypeScript 契约

`DemoPrincipal`、`LoginResponse`、`RouteResult` 把后端 JSON 变成编译期可检查的类型。`DemoRole` 使用字符串联合类型，错误角色会在构建时暴露。`import type` 只导入类型，不产生运行时代码。

Vite 当前模板启用了 TypeScript 的 `erasableSyntaxOnly`，所以 `constructor(public readonly status: number)` 这种参数属性被拒绝；`ApiError` 改成显式字段再在构造器赋值。

### 统一 API Client

`request<T>` 集中设置 JSON、Bearer Header、错误码和204空响应。工作台调用只发送 `{ message }`，测试明确断言请求体没有 `tenantId`。401、400 等响应统一变成 `ApiError(status, code)`，页面用同一个错误条展示。

### `sessionStorage`

Token 只保存在当前浏览器标签页，关闭标签页即清除，比长期 `localStorage` 更适合演示。XSS 仍可能读取它，因此生产系统应使用成熟 OIDC 流程、严格 CSP 和更合适的 Token/Session 策略。

Vite 只转换 TypeScript，不负责完整类型检查，所以 `npm run build` 先执行 `tsc -b`，再执行 `vite build`。[Vite TypeScript 说明](https://vite.dev/guide/features)

## 七、企业前端壳与响应式验证

Day 2 前端包含：

- 从后端动态加载的6个演示账号，覆盖两个业务租户和5种角色；
- 登录后显示受信租户、用户和角色；
- 按角色过滤的总览、工作台、订单、知识、审批、审计、Evaluation、MCP/Skill 菜单；
- 工作台路由预览和统一错误条；
- 900px 以下缩写侧栏、620px 以下顶部滚动导航与单列内容。

浏览器实测：客服 `service-a` 登录后只看到总览、AI 工作台、订单和知识库；路由问题返回 `ORDER / ORDERS / NOT_EXECUTED`；390×844 视口下导航、身份栏、表单和结果卡保持可读。

## 八、重点测试调用链

### `serverCatalogIssuesAndRevokesTrustedSession()`

目的：证明身份来自后端目录。准备：固定 Clock 和8小时 TTL。调用链：`login` → 随机 Token → `resolve` → `logout/advance`。断言：租户和角色正确，退出或过期后解析为空。结果原因：Token 只索引服务端 `StoredSession`。

### `loginCreatesTrustedSessionAndProtectsWorkbench()`

目的：验证完整 Spring Security 链。准备：真实 Spring 上下文，Mock 路由业务。调用链：账号目录 → 登录 → `/me` → 无 Token/伪造 Token 访问 → 有效 Token 访问 → 退出。断言：缺失和伪造 Token 都是401；有效 Token 返回 `tenant-a` 并可调用路由；退出后再次401。这个测试还验证 `127.0.0.1:5173` CORS。

### `loginThenRouteOrderQuestionThroughRealModel()`

目的：满足真实模型联调。准备：随机端口 Spring Boot、真实 DeepSeek Key。调用链：真实 HTTP 登录 → Bearer Filter → SecurityContext → 工作台 Controller → AI Service → DeepSeek → Java 路由结果。请求没有租户/角色字段。断言：Principal 是 `tenant-a` 的客服，结果是 `ORDER / ORDERS / NOT_EXECUTED`。

前端另有4个测试，重点看两类：权限导航确保客服看不到审批/审计/治理；API Client 确保受保护请求带 Bearer Token 且请求体只有 `message`。组件测试只验证登录页把 `accountId` 交给后端，不在前端生成身份。

## 九、测试命令与结果

```powershell
# 后端身份、CORS、Security Filter 与工作台契约
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent
mvn test "-Dtest=DemoSessionServiceTest,DemoAuthenticationIntegrationTest,WorkbenchRoutingServiceTest,WorkbenchRouteControllerTest"

# 真实 HTTP 登录 → Bearer → DeepSeek 路由
.\scripts\test-live.ps1 -Test DemoAuthenticatedWorkbenchLiveTest

# 前端组件、权限和 API Client；随后类型检查与生产构建
cd ..\enterprise-agent-web
npm test
npm run build
```

结果：后端6个专项测试通过；真实 DeepSeek LiveTest 1个通过；前端3个文件共4个测试通过；生产构建成功。全量 Java 回归239个测试、0失败、0错误，39个外部条件 LiveTest 在普通环境按设计跳过。

全量回归还发现一个测试隔离问题：引入 Spring Security 后，旧 `@WebMvcTest` 默认启用安全过滤，导致原本公开的学习期接口收到403。现在这些旧接口切片测试显式关闭 Filter；新 `/api/**` 的认证边界由 `DemoAuthenticationIntegrationTest` 完整验证。

## 十、当前边界与下一步

当前账号目录和会话都在内存，登录没有密码，定位是本地演示身份，不是生产认证系统。它已经保证前端不能自行改租户和角色，并能完整演示 Spring Security 调用链。Priority 2 会持久化身份相关数据；企业部署时再接 OIDC/SSO、密钥轮换、撤销和多实例共享。

Day 3 将从“已登录、能调用一次 JSON 路由”继续到“统一 AI 工作台的 SSE 流式回答、引用、步骤和失败状态”。

## 资料来源

- Level 1：Spring Security Servlet Architecture：https://docs.spring.io/spring-security/reference/servlet/architecture.html
- Level 1：Spring Security Authentication Architecture：https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html
- Level 1：Spring Security CORS：https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html
- Level 1：React Build from Scratch：https://react.dev/learn/build-a-react-app-from-scratch
- Level 1：Vite Getting Started：https://vite.dev/guide/
- Level 1：Vite TypeScript：https://vite.dev/guide/features
