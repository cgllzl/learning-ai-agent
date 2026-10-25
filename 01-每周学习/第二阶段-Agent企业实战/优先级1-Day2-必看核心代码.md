# Priority 1 Day 2：必看核心代码

> 建议阅读时间：30～35分钟
> 当天主笔记：`02-知识库/Agent安全/Phase2-Priority1-Day2-受信身份与企业前端壳.md`
> 真实联调：`.\scripts\test-live.ps1 -Test DemoAuthenticatedWorkbenchLiveTest`

## 一、今天读完要明白什么

前端选择的是账号 ID，不是权限。后端根据自己的账号目录签发随机 Token；后续请求通过 Filter 恢复 `DemoPrincipal`，再由 Spring Security 决定能否进入 `/api/**`。前端菜单只改善体验，不能代替后端授权。

## 二、只看这8个核心入口

先读后端1～5，再看前端6～8。

| 顺序 | 文件 / 方法 | 为什么必看 | 重点关注 | 暂时忽略 |
| --- | --- | --- | --- | --- |
| 1 | [DemoSessionService](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/identity/DemoSessionService.java) | 身份与租户的事实来源 | 账号目录、随机 Token、TTL、退出 | UI 文案 |
| 2 | [DemoBearerAuthenticationFilter](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/identity/DemoBearerAuthenticationFilter.java) | 看 Token 如何变成当前用户 | Header → resolve → SecurityContext | Servlet 样板方法 |
| 3 | [DemoSecurityConfig](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/identity/DemoSecurityConfig.java) | 看 URL 安全边界 | 公开登录、保护 `/api/**`、JSON 401、CORS | 旧接口细节 |
| 4 | [DemoAuthController](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/identity/DemoAuthController.java) | 看登录、`/me`、退出契约 | Controller 不接收租户/角色 | 普通 DTO |
| 5 | [DemoAuthenticationIntegrationTest](../../04-项目/enterprise-agent/src/test/java/com/enterprise/agent/identity/DemoAuthenticationIntegrationTest.java) | 一次看完整后端安全链 | 缺 Token、伪造、有效、退出、CORS | Spring 启动日志 |
| 6 | [api/client.ts](../../04-项目/enterprise-agent-web/src/api/client.ts) | 前端唯一 HTTP 出口 | Bearer Header、统一错误、请求体 | fetch 基础语法 |
| 7 | [permissions.ts](../../04-项目/enterprise-agent-web/src/auth/permissions.ts) | 看角色如何影响菜单体验 | 客服/主管/审计/管理员菜单 | CSS 与图标 |
| 8 | [App.tsx](../../04-项目/enterprise-agent-web/src/App.tsx) 与 [LoginPage.tsx](../../04-项目/enterprise-agent-web/src/components/LoginPage.tsx) | 看启动验证和登录交互 | `/accounts`、`/me`、sessionStorage | 视觉样式 |

## 三、完整调用链

```text
点击“客服小林”
  → LoginPage 只传 accountId=service-a
  → API Client POST /api/v1/auth/demo-login
  → DemoSessionService 从后端目录找到 tenant-a / CUSTOMER_SERVICE
  → 生成随机 Token 并保存服务端会话
  → App 把 Token 暂存 sessionStorage

提交工作台问题
  → API Client 加 Authorization: Bearer <token>
  → DemoBearerAuthenticationFilter 恢复 DemoPrincipal
  → SecurityContextHolder 保存 Authentication
  → SecurityFilterChain 允许已认证请求进入
  → @AuthenticationPrincipal 注入受信身份
  → DeepSeek 路由 → Java 白名单 → NOT_EXECUTED
```

缺 Token、伪造 Token、已注销或过期 Token 都停在 SecurityFilterChain，返回固定401 JSON，不进入 Controller。

## 四、核心代码怎么读

### 4.1 先看 `DemoSessionService`

`createAccounts()` 把账号、租户和角色写在后端。`login(accountId)` 只能从目录取 Principal；请求没有 `tenantId` 和 `roles` 参数。`SecureRandom` 生成32字节 Token；`ConcurrentHashMap` 保存 Token 到 Principal/到期时间的映射。

### 4.2 再看 Filter 和 SecurityConfig

Filter 只做认证恢复，不做订单权限判断。它把后端角色转为 `ROLE_*` authorities 并写入 SecurityContext。SecurityConfig 公开账号列表/登录，要求 `/api/**` 认证，并将 Filter 放在 `UsernamePasswordAuthenticationFilter` 前。当前演示没有用户名密码表单，因此排除 Boot 自动生成的默认用户。

### 4.3 看 Controller 如何拿身份

`@AuthenticationPrincipal DemoPrincipal principal` 来自 SecurityContext。它与 `@RequestBody WorkbenchRouteRequest` 是两个来源：前者受信，后者是不可信用户输入。后续业务查询必须使用前者的租户。

### 4.4 最后看前端

`api/client.ts` 统一附加 Bearer Token。`App.tsx` 打开页面时先取账号目录；如果标签页已有 Token，再用 `/me` 验证。`permissions.ts` 只决定菜单显示，业务安全仍在后端。

## 五、新语法 / API 速查

| 语法或 API | 解决什么问题 | 返回值 / 调用时机 | 常见误用 |
| --- | --- | --- | --- |
| `OncePerRequestFilter` | 每请求执行一次 Bearer 解析 | 进入 Controller 前 | Filter 里混入业务权限 |
| `SecurityContextHolder` | 保存当前请求 Authentication | Filter 验证成功后 | 放入前端自报的 Principal |
| `UsernamePasswordAuthenticationToken.authenticated(...)` | 构造已认证主体 | principal、credentials、authorities | 未验证 Token 就调用 |
| `SecurityFilterChain` | 配置 URL、Filter、401、CORS | 应用启动时构建 | 用前端隐藏按钮代替规则 |
| `@AuthenticationPrincipal` | 向 Controller 注入受信 Principal | Controller 参数 | 从请求体取 tenantId |
| `SecureRandom` | 生成不可预测 Token | 登录时 | 使用 UUID/普通 Random 代表生产安全 |
| `useEffect` | 页面启动后执行账号/会话检查 | React 挂载后 | 每次渲染重复登录 |
| `request<T>` | 用 TypeScript 表示响应类型 | Promise<T> | 认为泛型会做运行时 JSON 校验 |

注解参数必须是编译期常量：`@RequestAttribute(CONSTANT)` 可用字面量常量，不能用 `Class.getName()` 的运行时结果。

## 六、重点精读3个测试

### `serverCatalogIssuesAndRevokesTrustedSession()`

- 测试目的：验证后端账号目录、过期和退出。
- 准备条件：固定 Clock、8小时 TTL。
- 调用链：login → resolve → logout；再次 login → Clock 前进8小时 → resolve。
- 关键断言：租户/角色来自目录，退出和过期后为空。
- 为什么得到结果：Token 只索引服务端 StoredSession。

### `loginCreatesTrustedSessionAndProtectsWorkbench()`

- 测试目的：验证真实 Spring Security Filter Chain。
- 准备条件：完整 Spring 上下文，Mock DeepSeek 路由业务。
- 调用链：账号目录 → 登录 → `/me` → 无效访问 → 有效访问 → 退出。
- 关键断言：缺失/伪造 Token 401；有效 Token 返回 tenant-a；CORS 允许本地前端；退出后401。
- 为什么得到结果：只有 Filter 成功解析的 Token 会把 Principal 放入 SecurityContext。

### `loginThenRouteOrderQuestionThroughRealModel()`

- 测试目的：证明身份链与真实模型链能串起来。
- 准备条件：随机端口、DeepSeek Key、service-a。
- 调用链：真实 HTTP 登录 → Bearer → Filter → Controller → AI Service → DeepSeek。
- 关键断言：登录身份是 tenant-a 客服；路由是 ORDER/ORDERS/NOT_EXECUTED。
- 为什么得到结果：请求只带账号 ID 与问题，身份和分类分别由后端目录与模型/Java 规则产生。

前端4个测试只需读测试名：客服菜单过滤、管理员完整菜单、登录页只提交 accountId、API Client 只提交 message 并携带 Bearer。

## 七、今天可以先跳过

- `DemoLoginRequest/Response`、`DemoAccountSummary` 的普通 record 字段。
- 8份旧 Controller 测试新增的 `@AutoConfigureMockMvc(addFilters=false)`；这是兼容原学习期公开接口的机械调整。
- `styles.css` 的颜色、阴影和间距；只需知道900px与620px有响应式断点。
- Vite 模板生成的 tsconfig 细节；重点记住构建会执行 `tsc -b`。

## 八、自检问题

1. 为什么登录请求只传 accountId，不传 tenantId 和 roles？
2. Token 经过哪些组件才变成 `@AuthenticationPrincipal`？
3. 前端隐藏审批菜单能否阻止攻击者直接请求审批 API？
4. 为什么 `localhost:5173` 与 `127.0.0.1:5173` 要分别配置？
5. 当前演示会话距离生产 SSO 还缺哪些能力？
