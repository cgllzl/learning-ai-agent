package com.enterprise.agent.security;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 按租户隔离的订单数据（Day 2 演示用）。
 * 同一订单号 O1001 在两个租户里对应不同内容，用来证明租户之间互不可见。
 */
public class TenantScopedOrderData {

    private final Map<String, Map<String, String>> ordersByTenant = new LinkedHashMap<>();

    public TenantScopedOrderData() {
        put("t1", "O1001", "订单 O1001：用户 U1，商品 机械键盘，金额 399.0 元，状态 PAID");
        put("t2", "O1001", "订单 O1001：用户 U2，商品 27寸显示器，金额 1299.0 元，状态 SHIPPED");
        put("t2", "O2001", "订单 O2001：用户 U2，商品 鼠标垫，金额 59.0 元，状态 PENDING");
    }

    public void put(String tenantId, String orderId, String orderText) {
        ordersByTenant
                .computeIfAbsent(tenantId, ignored -> new LinkedHashMap<>())
                .put(orderId, orderText);
    }

    public Optional<String> find(String tenantId, String orderId) {
        return Optional.ofNullable(ordersByTenant.getOrDefault(tenantId, Map.of()).get(orderId));
    }
}
