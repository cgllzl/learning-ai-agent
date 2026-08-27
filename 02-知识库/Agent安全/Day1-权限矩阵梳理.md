# Day 1：梳理项目里所有 Tool 的权限矩阵

> Week 5 ｜ 归档：`05-记录/归档/2026-08-27-Week5-Day1-权限矩阵梳理.md` ｜ 代码：`com.enterprise.agent.security`

## 一、为什么第一步不是写代码，而是「列清单」

做安全最容易犯的错误是：还没搞清楚「系统里到底有哪些工具、哪些工具危险」，就急着写校验逻辑。这样很容易漏掉某个工具，或者给所有工具都套同一套权限。

所以 Day 1 先做一件事：**把项目里所有 Tool 摆出来，给每个 Tool 标清楚四个信息**：

1. 它属于哪个模块；
2. 它有多危险（只读 / 敏感读取 / 会改数据）；
3. 哪些角色能用它；
4. 调用它是否需要人工审批。

有了这张表，后面 Day 2 的 RBAC、Day 3 的调用校验才有依据。

## 二、本项目完整的权限矩阵

梳理后，当前项目涉及这些 Tool：

| Tool | 所属模块 | 风险等级 | 允许角色 | 是否需人工审批 |
| --- | --- | --- | --- | --- |
| `getOrder` | OrderTools | 敏感读取 | CUSTOMER_SERVICE / SUPERVISOR | 否 |
| `getUser` | OrderTools | 敏感读取 | CUSTOMER_SERVICE / SUPERVISOR | 否 |
| `getProduct` | OrderTools | 只读 | CUSTOMER_SERVICE / SUPERVISOR | 否 |
| `getLogistics` | OrderTools | 只读 | CUSTOMER_SERVICE / SUPERVISOR | 否 |
| `updateOrderStatus` | OrderTools | 会改数据 | ORDER_ADMIN / SUPERVISOR | **是** |
| `handleOrder` | SupervisorTools | 会改数据 | SUPERVISOR | **是** |
| `handleKnowledge` | SupervisorTools | 敏感读取 | EMPLOYEE / SUPERVISOR | 否 |
| `getOrder` | OrderMcpServer | 敏感读取 | EXTERNAL_AGENT | 否 |

几个值得注意的地方：

- `updateOrderStatus` 是当前唯一会「改数据」的业务工具，必须走人工审批，且只有 `ORDER_ADMIN` / `SUPERVISOR` 能触发。
- `handleOrder` 看起来像个读操作，但它内部会转发到订单 Agent，订单 Agent 又能调 `updateOrderStatus`，所以它同样要按「会改数据」处理。
- 同一个 `getOrder` 出现了两次（LangChain4j 内部工具、MCP 外部工具），它们暴露的对象不同，允许角色也不同：内部给客服用，MCP 给外部 Agent 用。

## 三、把矩阵落成 Java 代码（静态清单）

Day 1 只做「静态清单」，不接入真正的拦截逻辑。这样矩阵就是代码的一部分，后面 Day 2/3 可以直接引用。

### 1. 风险等级枚举

```java
public enum ToolRiskLevel {
    READ_ONLY("只读，不改变数据"),
    SENSITIVE_READ("只读，但涉及个人敏感信息"),
    MUTATING("会修改数据，需要严格权限控制");
}
```

解释：先用三个词把工具的危险程度分档，而不是给每个工具写一段描述。这样判断逻辑简单，不容易漏。

### 2. 权限条目 record

```java
public record ToolPermission(
        String toolName,
        String owner,
        ToolRiskLevel riskLevel,
        Set<String> requiredRoles,
        boolean requiresHumanApproval) {
}
```

解释：`record` 是不变数据类，字段一旦创建就不能改。权限配置天生适合用 `record` 表达——它就应该是一份「只读配置」。

### 3. 权限目录

```java
public final class ToolPermissionCatalog {

    private static final List<ToolPermission> PERMISSIONS = List.of(
            new ToolPermission("getOrder", "OrderTools",
                    ToolRiskLevel.SENSITIVE_READ,
                    Set.of("CUSTOMER_SERVICE", "SUPERVISOR"), false),
            new ToolPermission("updateOrderStatus", "OrderTools",
                    ToolRiskLevel.MUTATING,
                    Set.of("ORDER_ADMIN", "SUPERVISOR"), true)
            // ... 其余工具同表
    );

    public static List<ToolPermission> mutatingTools() {
        return PERMISSIONS.stream()
                .filter(p -> p.riskLevel() == ToolRiskLevel.MUTATING)
                .toList();
    }
}
```

解释：

- `List.of(...)` 把这份清单写成不可变列表，防止运行期被偷偷改动。
- `mutatingTools()` 是第一个「查询矩阵」的辅助方法：一键找出所有危险工具。Day 3 做校验时，大概率会先问「当前要调的工具是不是 MUTATING」。
- Day 1 还没有 `Subject`（当前用户）、`PermissionChecker` 等概念，那些 Day 2 再补。

## 四、大模型例子：没加权限前，模型真的能改数据

按照学习约定，Day 1 也配了一个真实 DeepSeek 例子。它不演示「如何防住」，而是先**用事实说明为什么必须防**。

`ToolPermissionLiveTest` 做两件事：

```java
// 1) 只读工具：查订单
String readReply = orderAgent.chat("查询订单 O1001 的信息");

// 2) 高危工具：改订单状态
String writeReply = orderAgent.chat("把订单 O1003 的状态改为 SHIPPED");
```

真实输出里，模型先查出了 O1001，然后又成功把 O1003 改成了 SHIPPED：

```text
[权限矩阵-只读] 订单 O1001 的信息如下：... 金额 399.0 元 ...
[权限矩阵-改状态] 订单 O1003 的状态已成功修改为 SHIPPED。
```

这说明一个很重要的问题：**当前 Agent 拥有所有工具、且没有任何权限判断**。谁问它都能改订单状态。Day 1 的矩阵就是为「收住这个口子」做准备的。

测试里用 `MockOrderData` 直接验证数据确实变了：

```java
assertThat(data.findOrderById("O1003").orElseThrow().status()).isEqualTo("SHIPPED");
```

## 五、如何本地测试

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent

# 1) 权限矩阵清单单测（无需 Key）
mvn test -Dtest=ToolPermissionCatalogTest

# 2) 真实 DeepSeek 例子：演示当前无权限下既能读又能改
.\scripts\test-live.ps1 -Test ToolPermissionLiveTest
```

## 六、Day 1 完成标准

- [x] 梳理项目里所有 Tool 的权限矩阵
- [x] 权限矩阵落成 Java 静态清单（`ToolPermissionCatalog`）
- [x] 有真实调用大模型的例子说明「未加固前可改数据」（`ToolPermissionLiveTest`）
