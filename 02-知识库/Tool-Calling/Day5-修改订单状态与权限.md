# Day 5：修改订单状态（参数校验与权限）

> Week 2 ｜ 代码：`com.enterprise.agent.agent.OrderTools.updateOrderStatus`

## 一、为什么"改状态"是危险的 Tool

- 副作用型工具（写库、改状态、发消息）一旦被模型误调/被提示注入利用，后果严重。
- 所以它必须做**参数校验 + 业务规则 + 最小权限**。

## 二、实现三重防线

```java
@Tool("修改订单状态。只有 PENDING 可改；新状态必须是 PAID/SHIPPED/DELIVERED/CANCELLED 之一")
public String updateOrderStatus(@P("订单号") String orderId,
                                @P("新状态，可选 PAID/SHIPPED/DELIVERED/CANCELLED") String newStatus) {
    // 1. 枚举校验：模型给的参数可能非法
    if (!VALID_STATUSES.contains(newStatus)) {
        return "非法状态 " + newStatus + "，允许值：...";
    }
    // 2. 订单存在性
    if (order == null) {
        return "未找到订单 " + orderId;
    }
    // 3. 业务规则：只有 PENDING 能改
    if (!"PENDING".equals(order.status())) {
        return "订单 " + orderId + " 当前状态为 " + order.status() + "，只有 PENDING 状态可以修改";
    }
    data.updateOrderStatus(orderId, newStatus);
    return "订单 " + orderId + " 状态已更新为 " + newStatus;
}
```

## 三、核心思想：安全边界在代码，不在模型承诺

- 模型只是"发起请求"，是否真的执行由 Java 代码决定。
- 即使模型被诱导去改非 PENDING 订单，代码也会拒绝。
- 这对应 Week 5「Agent 安全」的最小权限原则：**不要让 Agent 拥有所有 Tool 的权限**。

## 四、验证

`OrderToolsTest`：非法状态 / 未找到订单 / 非 PENDING 被拒 / PENDING 成功，4 种情况全覆盖。