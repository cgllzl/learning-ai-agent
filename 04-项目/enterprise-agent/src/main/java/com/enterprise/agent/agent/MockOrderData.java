package com.enterprise.agent.agent;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 内存模拟的企业订单数据（Week 2 Day 2：先只支持订单查询）。
 */
@Component
public class MockOrderData {

    public record Order(String id, String userId, String productId, String status, double amount) {
    }

    private final Map<String, Order> orders = new LinkedHashMap<>();

    public MockOrderData() {
        orders.put("O1001", new Order("O1001", "U1", "P1", "PAID", 399.0));
        orders.put("O1002", new Order("O1002", "U2", "P2", "SHIPPED", 1299.0));
        orders.put("O1003", new Order("O1003", "U1", "P3", "PENDING", 59.0));
    }

    public Optional<Order> findOrderById(String id) {
        return Optional.ofNullable(orders.get(id));
    }
}