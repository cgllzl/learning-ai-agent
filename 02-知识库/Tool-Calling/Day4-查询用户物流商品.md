# Day 4：扩展多工具（用户 / 物流 / 商品）

> Week 2 ｜ 代码：`com.enterprise.agent.agent.OrderTools`

## 一、多工具并存

```java
@Tool("根据用户 ID 查询用户信息")
public String getUser(@P("用户 ID，例如 U1") String userId) { ... }

@Tool("根据商品 ID 查询商品信息")
public String getProduct(@P("商品 ID，例如 P1") String productId) { ... }

@Tool("根据订单号查询物流信息")
public String getLogistics(@P("订单号，例如 O1002") String orderId) { ... }
```

- 多个工具注册给同一个 Agent，模型按**描述**自动选择。
- 数据源：`MockOrderData`（内存模拟），后续换真实数据库只需改这个类。

## 二、工具描述决定模型的选择（关键）

- 描述模糊：`查询信息` → 模型分不清该用哪个。
- 描述精确：`根据订单号查询物流信息` + 参数示例 `O1002` → 模型一次选对。
- 这是「模型如何理解你的能力」的唯一接口，值得花时间写清楚。

## 三、Agent 可以组合调用

用户：`帮我查一下 O1002 的物流`
→ 模型选择 `getLogistics("O1002")` → 返回「顺丰 SF123456789 运输中」（真实联调验证）

## 四、完成标准（部分）

- [x] 至少 4 个 Tool 可用：getOrder / getUser / getProduct / getLogistics
- [x] 模型能按意图自动调用（OrderAgentLiveTest 验证）