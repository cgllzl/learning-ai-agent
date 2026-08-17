# Day 4：扩展多工具（用户 / 物流 / 商品）

> Week 2 ｜ 归档：`05-记录/归档/2026-08-17-Week2-Day4-查询用户物流商品.md` ｜ 代码：`com.enterprise.agent.agent`

## 一、今天做什么

给订单 Agent 补上三类查询工具，让一个 Agent 拥有多个能力，模型按描述自动选择。

## 二、新增的三个工具

```java
@Tool("根据用户 ID 查询用户信息")
public String getUser(@P("用户 ID，例如 U1") String userId) { ... }

@Tool("根据商品 ID 查询商品信息")
public String getProduct(@P("商品 ID，例如 P1") String productId) { ... }

@Tool("根据订单号查询物流信息")
public String getLogistics(@P("订单号，例如 O1002") String orderId) { ... }
```

- 数据源 `MockOrderData` 同步扩展了 `User` / `Product` / `Logistics` 三张内存表。
- 加上 Day 2 的 `getOrder`，Agent 现在有 4 个工具。

## 三、工具描述决定模型的选择（关键）

- 描述模糊（如「查询信息」）→ 模型分不清该用哪个。
- 描述精确（如「根据订单号查询物流信息」+ 参数示例 `O1002`）→ 模型一次选对。
- 真实联调验证：
  - 「查一下用户 U1 的信息」→ 模型调 `getUser`，回复里出现「张三」
  - 「帮我查一下订单 O1002 的物流信息」→ 模型调 `getLogistics`，回复里出现「顺丰」
  - 「查询订单 O1001 的信息」→ 模型调 `getOrder`

## 四、完成标准

- [x] 至少 4 个 Tool 可用（getOrder / getUser / getProduct / getLogistics）
- [x] 模型能按意图自动调用正确的工具（真实联调验证）

## 五、下一步（Day 5）

- 修改订单状态（带参数校验与权限）——副作用型工具的三重防线。