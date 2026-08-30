# Day 3：Tool 权限校验 —— 模型只能调用有权限的工具

> Week 5 ｜ 归档：`05-记录/归档/2026-08-30-Week5-Day3-Tool权限校验.md` ｜ 代码：`com.enterprise.agent.security.PermissionAwareToolProvider`

## 一、先讲人话：这一步在解决什么

Day 2 已经做到：用户角色不对，连 Agent 的门都进不去。

但还有个更细的问题：就算用户进了门，他「能看见哪些工具」也应该按角色来。比如：

- `CUSTOMER_SERVICE` 能查订单，但**不能**改订单状态；
- `ORDER_ADMIN` 能改订单状态，但**不一定**需要看到所有查询工具。

如果把全部工具一次性都塞给模型，模型就可能在提示词攻击或误判下，调用一个当前用户不该用的工具。所以 Day 3 要做的是：**在把工具交给模型之前，先按当前用户角色过滤一遍。**

类比餐厅点菜：

- 服务员拿给普通顾客的是「顾客菜单」：只能点菜，不能进后厨。
- 经理拿的是「后厨菜单」：可以改菜、下架菜。
- 两份菜单都是同一家餐厅，但**摆在不同人面前的选项不同**。

## 二、LangChain4j 留给我们一个关键接口：ToolProvider

之前我们一直用：

```java
AiServices.builder(OrderAssistant.class)
        .chatModel(model)
        .tools(orderTools)   // 把所有工具一次性全给模型
        .build();
```

`tools(...)` 适合「静态工具集」，但它不会因为用户不同而变化。

LangChain4j 还提供了 `toolProvider(...)`，允许我们**每次自己决定返回哪些工具**：

```java
AiServices.builder(PermissionAwareOrderAssistant.class)
        .chatModel(model)
        .toolProvider(new PermissionAwareToolProvider(orderTools))
        .build();
```

`ToolProvider` 只有两个方法：

- `provideTools(request)`：返回这一次「允许模型使用的工具」。
- `isDynamic()`：返回 `true` 表示每次请求都重新计算。

## 三、核心实现：PermissionAwareToolProvider

```java
public class PermissionAwareToolProvider implements ToolProvider {

    private final Object toolsObject;

    public PermissionAwareToolProvider(Object toolsObject) {
        this.toolsObject = toolsObject;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        SecuritySubject subject = TenantContext.current();
        List<AiServiceTool> allTools = ToolService.findTools(toolsObject);

        List<AiServiceTool> allowedTools = allTools.stream()
                .filter(tool -> hasPermission(tool.name(), subject.roles()))
                .toList();

        return ToolProviderResult.builder()
                .addAll(allowedTools)
                .build();
    }

    private boolean hasPermission(String toolName, Set<String> roles) {
        return ToolPermissionCatalog.find(toolName)
                .map(permission -> permission.requiredRoles().stream().anyMatch(roles::contains))
                .orElse(false);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
```

逐段解释：

- `TenantContext.current()`：拿到 Day 2 放进 ThreadLocal 的当前用户。
- `ToolService.findTools(toolsObject)`：把 `OrderTools` 里所有 `@Tool` 方法反射出来，变成 LangChain4j 能理解的工具对象。
- `filter(hasPermission)`：只保留当前用户角色有权使用的工具。
- `hasPermission` 查的是 Day 1 的 `ToolPermissionCatalog`。如果某个工具没在权限矩阵里登记，直接拒绝——这就是「默认拒绝」的安全习惯。
- `isDynamic()` 返回 `true`：告诉框架「别缓存结果，每次请求都重新按用户算」。

## 四、把动态 Provider 接进 Agent

```java
public class PermissionAwareOrderAgentService {

    private final PermissionAwareOrderAssistant assistant;

    public PermissionAwareOrderAgentService(OpenAiChatModel chatModel, OrderTools orderTools) {
        this.assistant = AiServices.builder(PermissionAwareOrderAssistant.class)
                .chatModel(chatModel)
                .toolProvider(new PermissionAwareToolProvider(orderTools))
                .maxSequentialToolsInvocations(3)
                .build();
    }

    public String chat(SecuritySubject subject, String message) {
        return TenantContext.run(subject, () -> assistant.chat(message));
    }
}
```

注意顺序：

1. `TenantContext.run(...)` 先把当前用户放进去；
2. 模型要调工具时，框架会问 `PermissionAwareToolProvider`「现在能给我哪些工具」；
3. Provider 从 `TenantContext` 取用户，按角色过滤后返回；
4. 模型自然就「看不见」无权工具。

## 五、大模型例子：客服改不了，管理员能改

`PermissionAwareOrderLiveTest` 用真实 DeepSeek 跑了三个请求：

| 请求 | 用户角色 | 结果 |
| --- | --- | --- |
| 把 O1003 改为 SHIPPED | CUSTOMER_SERVICE | 模型说无法操作，数据保持 PENDING |
| 查询 O1001 | CUSTOMER_SERVICE | 正常返回 399 |
| 把 O1003 改为 SHIPPED | ORDER_ADMIN | 数据变成 SHIPPED |

真实输出中，客服角色自己承认没有修改能力：

```text
[客服尝试改状态] 很抱歉，我无法完成这个操作。我当前可用的工具只有查询功能...
[管理员改状态] 订单 O1003 的状态已成功更新为 SHIPPED。
```

这个例子能说明权限过滤是**真的生效**：

- 客服模型看不到 `updateOrderStatus`，所以它不会调用，数据也就没变。
- 管理员模型能看到 `updateOrderStatus`，所以它能调用，数据真的变了。

## 六、如何本地测试

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent

# 1) 不调大模型，验证按角色过滤工具
mvn test -Dtest=PermissionAwareToolProviderTest

# 2) 真实 DeepSeek 联调：客服改不了、管理员能改
.\scripts\test-live.ps1 -Test PermissionAwareOrderLiveTest
```

## 七、Day 3 完成标准

- [x] 模型只能看到当前用户角色有权使用的工具
- [x] 越权的工具被过滤，实际数据不被越权修改
- [x] 有真实调用大模型的例子（`PermissionAwareOrderLiveTest`）
