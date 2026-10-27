# Priority 1 Day 2：必看核心代码

> 建议阅读时间：45～55分钟
> 当天主笔记：`02-知识库/Agent安全/Phase2-Priority1-Day2-受信身份与企业前端壳.md`
> 本地核心测试：`mvn test -Dtest=DemoAuthenticationIntegrationTest`
> 真实模型联调：`.\scripts\test-live.ps1 -Test DemoAuthenticatedWorkbenchLiveTest`

## 一、今天读完要明白什么

Day 1 已经把问题送进了企业工作台，但那时还缺一个关键前提：后端怎样知道“请求者是谁”。Day 2 因此自然引出认证链。

可以把它想成进入办公楼：

1. `DemoSessionService` 是前台，核对内部账号并发门禁卡 Token。
2. `DemoBearerAuthenticationFilter` 是门口读卡器，读取 Token 并恢复员工身份。
3. `DemoSecurityConfig` 是门禁规则，决定哪些门公开、哪些门必须先刷卡。
4. Controller 是具体办公室，只相信门禁系统传来的身份，不相信访客自己填写的租户和角色。

所以，前端选择的是账号 ID，不是权限。后端根据自己的账号目录签发随机 Token；后续请求通过 Filter 恢复 `DemoPrincipal`，再由 Spring Security 决定能否进入 `/api/**`。前端菜单只改善体验，不能代替后端授权。

## 二、今天最值得看的核心代码

先读后端 1～5，再选择性看前端 6～8。如果时间紧，只精读 1～5。

| 顺序 | 文件 / 方法 | 为什么必看 | 重点关注 | 暂时忽略 |
| --- | --- | --- | --- | --- |
| 1 | [DemoSessionService](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/identity/DemoSessionService.java) | 身份与租户的事实来源 | 账号目录、随机 Token、TTL、退出 | UI 文案 |
| 2 | [DemoBearerAuthenticationFilter](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/identity/DemoBearerAuthenticationFilter.java) | 看 Token 如何变成当前用户 | Header → resolve → Authentication → SecurityContext | Servlet 普通取值语法 |
| 3 | [DemoSecurityConfig](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/identity/DemoSecurityConfig.java) | 看 URL 安全边界如何生效 | 规则顺序、无状态、401、CORS、Filter 位置 | 旧接口细节 |
| 4 | [DemoAuthController](../../04-项目/enterprise-agent/src/main/java/com/enterprise/agent/identity/DemoAuthController.java) | 看登录、`/me`、退出契约 | Controller 不接收租户/角色 | 普通 DTO |
| 5 | [DemoAuthenticationIntegrationTest](../../04-项目/enterprise-agent/src/test/java/com/enterprise/agent/identity/DemoAuthenticationIntegrationTest.java) | 一次看完整后端安全链 | `@MockitoBean`、`when`、`verify`、缺 Token、伪造、有效、退出、CORS | Spring 启动日志 |
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
  → DemoBearerAuthenticationFilter 读取并解析 Token
  → DemoSessionService.resolve(token) 恢复 DemoPrincipal
  → Filter 构造 Authentication 并写入 SecurityContextHolder
  → DemoSecurityConfig 的 /api/** 规则确认“当前请求已认证”
  → Controller 的 @AuthenticationPrincipal 收到受信身份
  → DeepSeek 路由 → Java 白名单 → NOT_EXECUTED
```

缺 Token、伪造 Token、已注销或过期 Token 都无法产生 `Authentication`。Filter 仍把请求交给后续过滤器，后面的授权过滤器发现 `/api/**` 要求认证，才通过 `AuthenticationEntryPoint` 返回固定的 401 JSON。也就是说：

```text
Filter 负责“尽力认出你”
SecurityConfig 负责“认不出来时是否还能进”
```

## 四、核心代码与语法拆解

### 4.1 `DemoSessionService`：后端身份事实来源

`createAccounts()` 把账号、租户和角色写在后端。`login(accountId)` 只能从目录取 Principal；请求没有 `tenantId` 和 `roles` 参数。`SecureRandom` 生成 32 字节 Token；`ConcurrentHashMap` 保存 Token 到 Principal/到期时间的映射。

这一步先回答“Token 是否可信”，下一步 Filter 才能把可信 Token 转成 Spring Security 能理解的当前用户。

### 4.2 `DemoBearerAuthenticationFilter`：把门禁卡恢复成当前用户

#### 4.2.1 类声明与构造器注入

```java
public class DemoBearerAuthenticationFilter extends OncePerRequestFilter {
    private final DemoSessionService sessionService;

    public DemoBearerAuthenticationFilter(DemoSessionService sessionService) {
        this.sessionService = sessionService;
    }
}
```

- `extends OncePerRequestFilter`：继承 Spring 提供的过滤器基类。一次请求即使经过 Servlet 的不同分派阶段，也能避免同一个 Filter 的主要逻辑被无意执行多次。
- `private final`：依赖创建后不可重新赋值。
- 构造器注入：Filter 不自己创建 `DemoSessionService`，而是由配置类把应用中的真实服务传进来。这样依赖清晰，也容易测试。
- 这里没有加 `@Component`。Filter 是由 `DemoSecurityConfig` 明确加入安全过滤链的，可避免它同时被 Servlet 容器和 Spring Security 重复注册。

#### 4.2.2 为什么请求属性名要定义成常量

```java
public static final String ACCESS_TOKEN_ATTRIBUTE =
        "com.enterprise.agent.identity.demoAccessToken";
```

Filter 验证成功后把 Token 放入本次请求，退出接口再用：

```java
@RequestAttribute(DemoBearerAuthenticationFilter.ACCESS_TOKEN_ATTRIBUTE)
String accessToken
```

`@RequestAttribute` 的注解参数必须是编译期常量，所以这里使用字符串常量，不能在注解里调用 `DemoBearerAuthenticationFilter.class.getName()`。名字写成较长的命名空间形式，是为了降低与别的请求属性重名的概率。

请求属性只活在当前 HTTP 请求中，不是全局变量，也不是浏览器 Cookie。

#### 4.2.3 `doFilterInternal` 方法签名

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain)
        throws ServletException, IOException {
```

- `@Override`：告诉编译器这是在重写父类规定的方法；方法名或参数写错会直接编译失败。
- `protected`：与父类约定的可见性一致，供框架调用，并不是 Controller 主动调用它。
- `HttpServletRequest`：读取 Header、路径和请求属性。
- `HttpServletResponse`：必要时可写响应；当前 Filter 不直接返回 401。
- `FilterChain`：代表后续过滤器和最终 Controller 的调用链。
- `throws ServletException, IOException`：过滤器链或网络读写可能抛出的受检异常，交由 Web 容器处理。

调用时机是在 Controller 之前，每个进入 Spring Security 的请求都会经过它。

#### 4.2.4 提取 Bearer Token

```java
String token = bearerToken(request.getHeader("Authorization"));
```

典型请求头是：

```http
Authorization: Bearer abc123
```

辅助方法的逻辑是：Header 不存在、不是以 `Bearer ` 开头，或后面的 Token 为空，都返回 `null`。`substring("Bearer ".length()).trim()` 先去掉固定前缀，再去掉 Token 两端空格。

当前实现对 `Bearer ` 的大小写敏感，适合本阶段受控客户端；企业网关场景还要统一协议兼容、日志脱敏和异常格式。

#### 4.2.5 为什么 `if` 有两个条件

```java
if (token != null
        && SecurityContextHolder.getContext().getAuthentication() == null) {
```

- `token != null`：只有格式正确且非空的 Token 才值得查会话。
- `getAuthentication() == null`：如果前面的可信认证机制已经确认了身份，就不应由本演示 Filter 覆盖它。
- `&&` 是短路与：左边为 `false` 时不再执行右边。

常见误用是拿到任意字符串就直接创建已认证对象。正确顺序必须是：先查服务端会话并验证，再创建 Authentication。

#### 4.2.6 `Optional.ifPresent`：只在会话有效时继续

```java
sessionService.resolve(token).ifPresent(principal -> {
    // 只有有效、未过期、未注销的 Token 才会进入这里
});
```

`resolve(token)` 返回 `Optional<DemoPrincipal>`：

- 有值：Token 有效，Lambda 参数 `principal` 就是后端恢复出的身份。
- 空值：Token 伪造、过期或已注销，Lambda 完全不执行。

这里用 `ifPresent` 表达“有可信身份才写安全上下文”。它没有返回新的值；如果后续需要继续转换数据，才考虑 `map`。

#### 4.2.7 从业务角色转换为 Spring Authority

```java
List<SimpleGrantedAuthority> authorities = principal.roles().stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
        .toList();
```

这是一条集合转换流水线：

1. `principal.roles()` 得到业务角色集合，例如 `CUSTOMER_SERVICE`。
2. `.stream()` 开启逐个转换。
3. `.map(...)` 把每个业务角色变成 Spring Security 认识的权限对象。
4. `.toList()` 收集为列表；Java 21 中这个结果不应被当作可修改列表。

`ROLE_` 前缀很重要：以后使用 `hasRole("CUSTOMER_SERVICE")` 时，Spring 会按约定寻找 `ROLE_CUSTOMER_SERVICE`。如果未来改用 `hasAuthority`，则要传完整 Authority 字符串，不能混淆“角色”和“权限”。

#### 4.2.8 构造“已经认证”的 Authentication

```java
var authentication = UsernamePasswordAuthenticationToken.authenticated(
        principal, token, authorities);
```

虽然类名里有 `UsernamePassword`，它本质上仍是一个常用的 `Authentication` 实现，这里承载的是 Bearer 会话身份：

- 第一个参数 `principal`：当前用户是谁，后面会被 `@AuthenticationPrincipal` 取出。
- 第二个参数 `token`：credentials，即本次认证凭据。当前演示用于表达认证来源；生产代码应避免输出或长期保留敏感凭据。
- 第三个参数 `authorities`：当前用户拥有哪些角色/权限。
- 返回值：`UsernamePasswordAuthenticationToken`，并且状态已经是 authenticated。

为什么能调用 `authenticated(...)`？因为前一行 `sessionService.resolve(token)` 已完成可信验证。若尚未验证，应使用未认证形态，不能自己把攻击者输入标成已认证。

`var` 只让编译器根据右侧推断局部变量类型，不代表动态类型；这里实际类型仍是 `UsernamePasswordAuthenticationToken`。

#### 4.2.9 写入当前请求的安全上下文

```java
SecurityContextHolder.getContext().setAuthentication(authentication);
request.setAttribute(ACCESS_TOKEN_ATTRIBUTE, token);
```

- `SecurityContextHolder` 保存当前执行线程所对应的安全上下文。
- 后续授权过滤器从中判断请求是否 authenticated。
- Controller 的 `@AuthenticationPrincipal` 从 Authentication 中取出 `principal`。
- 请求属性保存原 Token，供 `/logout` 删除服务端会话。

不要把前端请求体里的 `tenantId` 或 `roles` 放进这里。只有后端根据 Token 恢复出的 Principal 才是受信身份。

#### 4.2.10 为什么最后一定要 `filterChain.doFilter`

```java
filterChain.doFilter(request, response);
```

这相当于门卫完成检查后说“交给下一道门”。如果漏掉这一句，请求会停在当前 Filter，后续授权规则和 Controller 都不会执行。

伪造 Token 时，本 Filter 也会继续调用它，而不是自己返回 401。后续会发生：

```text
resolve 返回 Optional.empty
  → SecurityContext 中没有 Authentication
  → /api/** 的 authenticated() 规则不满足
  → AuthenticationEntryPoint 写出 401 JSON
```

因此，“Filter 没直接抛异常”不等于伪造 Token 被放行。

### 4.3 `DemoSecurityConfig`：决定哪些门需要刷卡

Filter 解决“能否识别身份”，现在配置类接着解决“每个 URL 需要什么身份”。

#### 4.3.1 类上的两个注解

```java
@Configuration
@EnableConfigurationProperties(DemoSessionProperties.class)
public class DemoSecurityConfig {
```

- `@Configuration`：告诉 Spring 这是配置类，里面的 `@Bean` 方法用于创建和装配对象。
- `@EnableConfigurationProperties(DemoSessionProperties.class)`：让 `DemoSessionProperties` 参与配置属性绑定，例如从配置文件读取会话 TTL。

#### 4.3.2 `SecurityFilterChain` Bean 与 `HttpSecurity`

```java
@Bean
SecurityFilterChain apiSecurity(HttpSecurity http,
                                DemoSessionService sessionService) throws Exception {
```

- `@Bean`：方法返回对象交给 Spring 容器管理。
- `HttpSecurity`：Spring 注入的安全规则 Builder，用链式 DSL 逐步配置 CSRF、CORS、会话、URL 授权和 Filter。
- `DemoSessionService`：传给自定义 Bearer Filter。
- 返回 `SecurityFilterChain`：应用启动时构建好的过滤器链，不是每次请求重新 build。

`http.xxx(...).yyy(...)` 每一步通常都返回 Builder 本身，所以能连续配置。Lambda 参数如 `authorize`、`session` 是对应配置器，不是业务数据。

#### 4.3.3 CSRF、CORS 与无状态配置不是一回事

```java
.csrf(AbstractHttpConfigurer::disable)
.cors(cors -> { })
.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

- `AbstractHttpConfigurer::disable` 是方法引用，可理解为 `configurer -> configurer.disable()`。当前认证凭据放在 `Authorization` Header，不依赖浏览器自动携带的登录 Cookie，因此演示 API 关闭 CSRF。
- 这不代表“CSRF 永远不重要”。如果以后改为 Cookie 会话，必须重新评估并启用合适的 CSRF 防护。
- `.cors(cors -> { })` 开启 Spring Security 的 CORS 集成，具体允许哪些来源由后面的 `CorsConfigurationSource` Bean 提供。
- `STATELESS` 表示 Spring Security 不把 Authentication 保存到 `HttpSession`，每次请求都重新解析 Bearer Token。

容易混淆的一点：本项目的 `DemoSessionService` 仍用 Map 保存 Token 会话，因此整个应用并非真正“无服务端状态”。这里的 `STATELESS` 只是在说 Spring Security 不使用 Servlet `HttpSession` 保存登录状态。

#### 4.3.4 URL 规则按顺序匹配

```java
.authorizeHttpRequests(authorize -> authorize
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers(
                "/api/v1/auth/demo-accounts",
                "/api/v1/auth/demo-login",
                "/actuator/**").permitAll()
        .requestMatchers("/api/**").authenticated()
        .anyRequest().permitAll())
```

可以按“从特殊到一般”阅读：

1. 所有 `OPTIONS` 预检请求公开，否则浏览器跨域正式请求可能还没发出就失败。
2. 账号列表、登录和健康检查公开；不登录就必须能访问登录入口。
3. 其余 `/api/**` 必须已认证。
4. 其他请求暂时公开，兼容当前非 API 资源和前期学习接口。

规则顺序很关键。若先写 `requestMatchers("/api/**").authenticated()`，后面的公开登录规则会被更宽泛的规则提前匹配，用户连登录接口都进不去。

`permitAll()` 是无需认证；`authenticated()` 只要求已经认证，还没有细分客服、主管和审计员。后续企业权限阶段会继续增加 `hasRole`、方法级授权和租户数据约束。

#### 4.3.5 `AuthenticationEntryPoint` 如何生成固定 401

```java
.exceptionHandling(errors -> errors.authenticationEntryPoint(
        (request, response, error) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\"}");
        }))
```

`authenticationEntryPoint` 处理“访问受保护资源但当前没有有效认证”的情况。三参数 Lambda 分别是请求、响应和认证异常。本例依次设置：

- HTTP 状态码 401；
- UTF-8 字符集；
- JSON 内容类型；
- 固定错误体 `{"error":"UNAUTHORIZED"}`。

这样前端拿到的是稳定契约，不会跳转到默认登录页，也不会收到一段 HTML。401 表示尚未认证；以后“已认证但无权限”通常应返回 403，并由 AccessDenied 处理器负责。

#### 4.3.6 为什么 Filter 放在这个位置

```java
.addFilterBefore(
        new DemoBearerAuthenticationFilter(sessionService),
        UsernamePasswordAuthenticationFilter.class);

return http.build();
```

- `new DemoBearerAuthenticationFilter(sessionService)`：显式创建本项目 Filter，并传入 Spring 管理的会话服务。
- `addFilterBefore(A, B.class)`：把 A 插在 B 之前。这样后续授权逻辑执行前，Bearer 身份已经有机会写入 SecurityContext。
- 第二个参数主要作为 Spring Security 过滤器链中的位置锚点，并不表示项目一定使用表单用户名密码登录。
- `http.build()`：在应用启动阶段把规则组装成最终 `SecurityFilterChain` Bean。

常见误用包括：Filter 既标 `@Component` 又 `addFilterBefore` 导致重复执行；把 Filter 放在授权之后导致身份恢复太晚；在 Filter 中直接编写订单、审批等业务权限。

#### 4.3.7 CORS Bean 逐句理解

```java
@Bean
CorsConfigurationSource corsConfigurationSource(
        @Value("${app.frontend-origins:http://localhost:5173,http://127.0.0.1:5173}")
        String frontendOrigins) {
```

`@Value` 先读取 `app.frontend-origins`；冒号后是未配置时的默认值。`localhost:5173` 与 `127.0.0.1:5173` 在浏览器眼中是两个不同 Origin，所以本地都列出。

```java
configuration.setAllowedOrigins(Arrays.stream(frontendOrigins.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .toList());
```

这条流的含义是：按逗号拆分 → 去掉每项空格 → 丢弃空项 → 收集为允许来源列表。`String::trim` 是方法引用，等价于 `value -> value.trim()`。

```java
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of(
        HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
configuration.setExposedHeaders(List.of(HttpHeaders.WWW_AUTHENTICATE));
configuration.setAllowCredentials(false);
```

- Allowed Methods：前端可以跨域使用哪些 HTTP 方法。
- Allowed Headers：前端请求允许携带哪些 Header，Bearer Token 需要 `Authorization`。
- Exposed Headers：哪些响应 Header 可以被浏览器 JavaScript 读取。
- `allowCredentials(false)`：不允许 Cookie 等浏览器 credentials 模式；当前 Bearer Header 由前端显式添加。

```java
UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
source.registerCorsConfiguration("/api/**", configuration);
return source;
```

最后只把这套跨域规则绑定到 `/api/**`。生产环境应从部署配置传入准确的 HTTPS 域名，不能为了省事开放任意 Origin。

### 4.4 `DemoAuthController`：Controller 只拿受信身份

`@AuthenticationPrincipal DemoPrincipal principal` 来自 SecurityContext。它与 `@RequestBody WorkbenchRouteRequest` 是两个来源：前者是后端验证得到的受信身份，后者是不可信用户输入。后续业务查询必须使用前者的租户。

退出时使用 `@RequestAttribute` 获取 Filter 保存的 Token，再由 `DemoSessionService.logout` 删除服务端会话。下次携带同一个 Token 时，Filter 的 `resolve` 得不到 Principal，最终返回 401。

### 4.5 前端只负责携带凭据和改善体验

`api/client.ts` 统一附加 Bearer Token。`App.tsx` 打开页面时先取账号目录；如果标签页已有 Token，再用 `/me` 验证。`permissions.ts` 只决定菜单显示，业务安全仍在后端。

## 五、`DemoAuthenticationIntegrationTest` 的 Mockito 与 MockMvc 语法

### 5.1 先分清测试里哪些是真的、哪个是假的

```java
@SpringBootTest
@AutoConfigureMockMvc
class DemoAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkbenchRoutingService routingService;
}
```

| 对象 | 真/假 | 作用 |
| --- | --- | --- |
| `SecurityFilterChain`、Filter、SessionService、Controller | 真实 | 真正验证身份、安全规则、会话与 JSON 契约 |
| `MockMvc` | 真实的 Spring 测试工具 | 在进程内发起模拟 HTTP 请求，会真的经过 Spring MVC 和安全 Filter；它不是 Mockito Mock |
| `ObjectMapper` | 真实 | 把登录响应 JSON 解析成 `JsonNode`，以便提取动态 Token |
| `WorkbenchRoutingService` | Mockito 假对象 | 隔离真实大模型，避免测试受网络、费用和随机输出影响 |

`@SpringBootTest` 启动完整 Spring 应用上下文。`@AutoConfigureMockMvc` 创建 `MockMvc` 测试入口，不需要真的监听一个 TCP 端口。

### 5.2 `@MockitoBean` 到底做了什么

```java
@MockitoBean
private WorkbenchRoutingService routingService;
```

它不是仅仅给字段赋一个普通 Mock，而是找到 Spring 容器中的 `WorkbenchRoutingService` Bean，并用 Mockito Mock 替换它。于是 Controller 注入到的也是这个 Mock。

这与直接写 `WorkbenchRoutingService routingService = mock(...)` 的区别是：后者只创建一个测试变量，Spring 中的 Controller 仍可能拿到真实 Bean；`@MockitoBean` 才能影响整个测试 ApplicationContext 的依赖注入。

在当前 Spring Boot 3.5 / Spring Test 版本中使用 `@MockitoBean`。旧教程常见 `@MockBean`，不要在本项目里机械混用。这里只 Mock 外部不确定边界——大模型路由；认证主链路必须保留真实，否则安全集成测试就失去意义。

### 5.3 `when(...).thenReturn(...)`：规定 Mock 被调用时返回什么

```java
when(routingService.route("查询订单 O1001"))
        .thenReturn(new WorkbenchRouteResult(
                TicketCategory.ORDER,
                WorkbenchView.ORDERS,
                false,
                "NOT_EXECUTED"));
```

可以读成：“当 `routingService.route` 收到完全相同的字符串时，就返回指定结果。”

- `when(...)`：选择要模拟的方法调用。
- `.thenReturn(...)`：设置返回值。
- 真实 `WorkbenchRoutingService.route` 不会执行，因此不会调用 DeepSeek。
- 这里使用精确参数匹配；若实际字符串不同，这条桩不会命中，对象返回类型通常得到 Mockito 默认值 `null`。
- Stubbing 只规定行为，不代表方法已经在业务调用链中执行过。是否真的调用，要用 `verify` 检查。

常见误用：在请求执行之后才 `when`，会来不及影响这次请求；或者过度使用 `anyString()`，导致错误参数也能通过测试。

### 5.4 `verify(..., never())`：证明被安全层挡住了

```java
verify(routingService, never()).route(anyString());
```

这句位于“缺 Token”和“伪造 Token”两次请求之后，含义是：到目前为止，`routingService.route` 对任意非 null 字符串的调用次数必须为 0。

- `verify(mock, mode)`：检查 Mock 的历史调用记录，不会再执行业务方法。
- `never()`：等价于 `times(0)`。
- `anyString()`：Mockito 参数匹配器，表示任意非 null 字符串。

它证明 401 不只是响应看起来正确，而且请求确实没有穿透安全链进入大模型路由服务。这是企业安全测试很重要的“负向证据”。

参数匹配器还有一个规则：同一次方法调用如果某个参数用了 Matcher，其他参数通常也要用 Matcher，例如 `eq("tenant-a")`，不要把 Matcher 和普通值随意混在一起。

### 5.5 `verify(mock).method(...)`：证明合法请求进入一次业务层

```java
verify(routingService).route("查询订单 O1001");
```

没有显式次数时，默认等价于：

```java
verify(routingService, times(1)).route("查询订单 O1001");
```

它放在合法 Token 请求之后，证明请求确实穿过 Filter 和授权规则，并且 Controller 只调用了一次路由服务。结合上一条 `never()`，完整证据就是：非法身份 0 次，合法身份 1 次。

`verify` 检查“有没有调用”；`assert/andExpect` 检查“结果是什么”，两者解决的问题不同。

### 5.6 `MockMvc.perform(...).andExpect(...)`：在进程内走真实 Web 调用链

```java
mockMvc.perform(post("/api/v1/workbench/route")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"查询订单 O1001\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suggestedView").value("ORDERS"));
```

- `post(path)`：创建 POST 请求。
- `.header(...)`：添加 Bearer Token。
- `.contentType(...)`：声明请求体为 JSON。
- `.content(...)`：设置请求体。
- `mockMvc.perform(...)`：执行请求，真实经过 Filter → Security → Controller。
- `.andExpect(...)`：对响应连续断言。
- `jsonPath("$.suggestedView")`：从 JSON 根对象读取 `suggestedView` 字段。

登录响应中的 Token 每次随机生成，所以不能把它写死：

```java
String loginBody = mockMvc.perform(...)
        .andReturn().getResponse().getContentAsString();
JsonNode login = objectMapper.readTree(loginBody);
String token = login.path("accessToken").asText();
```

`andReturn()` 取得完整执行结果，`getResponse()` 取得响应，`getContentAsString()` 取得 JSON 字符串。`readTree` 把 JSON 解析成树，`path` 安全定位字段，`asText` 转为字符串。

## 六、重点精读 3 个测试

### 6.1 `serverCatalogIssuesAndRevokesTrustedSession()`

- 测试目的：验证后端账号目录、过期和退出。
- 准备条件：固定 Clock、8 小时 TTL。
- 调用链：`login → resolve → logout`；再次 `login → Clock 前进 8 小时 → resolve`。
- 关键断言：租户/角色来自目录，退出和过期后为空。
- 为什么得到结果：Token 只索引服务端 `StoredSession`；记录被移除或到期后无法恢复 Principal。

### 6.2 `loginCreatesTrustedSessionAndProtectsWorkbench()`

- 测试目的：验证真实 Spring Security Filter Chain，并证明非法请求没有进入业务层。
- 准备条件：完整 Spring 上下文；仅把 DeepSeek 路由边界替换为 `@MockitoBean`。
- 调用链：账号目录 → 登录 → 解析动态 Token → `/me` → 缺 Token → 伪造 Token → 有效 Token → 退出 → 再访问 `/me`。
- 关键断言：CORS 允许本地前端；登录身份为 `tenant-a`；缺失/伪造 Token 返回 401；`verify(..., never())` 证明两次非法请求没有调用路由服务；有效请求返回 `ORDERS`；`verify(...)` 证明路由服务调用一次；退出后原 Token 返回 401。
- 为什么得到结果：只有 `DemoSessionService.resolve` 成功，Filter 才会把 Principal 写入 SecurityContext；`authenticated()` 才会放行请求。退出删除会话后，相同 Token 再也无法恢复身份。

最值得顺着读的一段链路：

```text
无 Token / forged-token
  → Filter 无法设置 Authentication
  → /api/** 要求 authenticated
  → AuthenticationEntryPoint 返回 401
  → verify(routingService, never()) 成立

合法 Token
  → Filter resolve 成功并设置 Authentication
  → /api/** 放行
  → Controller 调 routingService.route
  → Mock 返回 ORDER / ORDERS
  → 响应 200，verify 默认 times(1) 成立
```

### 6.3 `loginThenRouteOrderQuestionThroughRealModel()`

- 测试目的：证明身份链与真实模型链能串起来。
- 准备条件：随机端口、有效 DeepSeek Key、`service-a`。
- 调用链：真实 HTTP 登录 → Bearer → Filter → Controller → AI Service → DeepSeek。
- 关键断言：登录身份是 `tenant-a` 客服；路由是 `ORDER/ORDERS/NOT_EXECUTED`。
- 为什么得到结果：请求只带账号 ID 与问题，身份和分类分别由后端目录与模型/Java 规则产生。

集成测试负责稳定、快速地覆盖安全规则，LiveTest 负责证明真实大模型可调用。两者不能互相替代。

前端 4 个测试只需先读测试名：客服菜单过滤、管理员完整菜单、登录页只提交 accountId、API Client 只提交 message 并携带 Bearer。

## 七、新语法 / API 速查

| 语法或 API | 解决什么问题 | 返回值 / 调用时机 | 常见误用 |
| --- | --- | --- | --- |
| `OncePerRequestFilter` | 每请求执行一次 Bearer 解析 | Controller 前 | Filter 里混入订单等业务权限 |
| `filterChain.doFilter` | 把请求交给后续 Filter/Controller | 当前 Filter 处理完后 | 忘记调用导致请求中断 |
| `SecurityContextHolder` | 保存当前请求 Authentication | Token 验证成功后 | 放入前端自报的 Principal |
| `UsernamePasswordAuthenticationToken.authenticated(...)` | 构造已认证主体 | principal、credentials、authorities | 未验证 Token 就调用 |
| `SecurityFilterChain` | 配置 URL、Filter、401、CORS | 应用启动时构建 | 用前端隐藏按钮代替规则 |
| `requestMatchers` | 给不同 URL/方法设置规则 | 按声明顺序匹配 | 宽泛规则放在公开登录规则前 |
| `AuthenticationEntryPoint` | 为未认证访问生成 401 | 授权失败时 | 与已认证但无权的 403 混淆 |
| `CorsConfigurationSource` | 定义允许的前端来源和 Header | 浏览器跨域请求时 | 生产环境开放任意 Origin |
| `@AuthenticationPrincipal` | 向 Controller 注入受信 Principal | Controller 参数 | 从请求体取 tenantId |
| `@MockitoBean` | 用 Mock 替换 Spring Bean | 测试上下文启动时 | 把要验证的安全主链路也 Mock 掉 |
| `when(...).thenReturn(...)` | 给 Mock 预设返回值 | 被调用且参数匹配时 | 误以为 Stubbing 证明方法已执行 |
| `verify(..., never())` | 验证方法从未执行 | 请求执行后 | 只断言 401，不验证业务层未被触达 |
| `MockMvc` | 不开真实端口测试 Spring Web 链 | 测试执行请求时 | 误认为它是 Mockito Mock |
| `SecureRandom` | 生成不可预测 Token | 登录时 | 使用普通 `Random` 生成安全凭据 |
| `useEffect` | 页面启动后执行账号/会话检查 | React 挂载后 | 每次渲染重复登录 |
| `request<T>` | 用 TypeScript 表示响应类型 | `Promise<T>` | 认为泛型会做运行时 JSON 校验 |

## 八、今天可以先跳过

- `DemoLoginRequest/Response`、`DemoAccountSummary` 的普通 record 字段。
- 8 份旧 Controller 测试新增的 `@AutoConfigureMockMvc(addFilters=false)`；这是兼容前期公开接口的机械调整。
- `styles.css` 的颜色、阴影和间距；只需知道 900px 与 620px 有响应式断点。
- Vite 模板生成的 tsconfig 细节；重点记住构建会执行 `tsc -b`。
- `DemoAuthenticationIntegrationTest` 中重复的 `.contentType/.content` 样板；先抓住 Token 是否存在与 `verify` 断言。

## 九、自检问题

1. 为什么登录请求只传 `accountId`，不传 `tenantId` 和 `roles`？
2. Token 经过哪些组件才变成 `@AuthenticationPrincipal`？
3. 为什么伪造 Token 时 Filter 不抛异常，最后仍会得到 401？
4. `STATELESS` 为什么不等于整个演示系统没有服务端会话？
5. 为什么公开登录规则必须写在 `/api/**` 规则之前？
6. `when` 与 `verify` 分别回答什么问题？
7. 为什么 `MockMvc` 不是 Mockito Mock？测试中哪些组件仍然是真实的？
8. 前端隐藏审批菜单能否阻止攻击者直接请求审批 API？
9. 为什么 `localhost:5173` 与 `127.0.0.1:5173` 要分别配置？
10. 当前演示会话距离生产 SSO 还缺哪些能力？
