package com.enterprise.agent.agent;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 内存模拟的企业订单数据（Week 2 Day 5：订单 / 用户 / 商品 / 物流 + 状态修改）。
 */
@Component
public class MockOrderData {

    public record Order(String id, String userId, String productId, String status, double amount) {
    }

    public record User(String id, String name, String phone) {
    }

    public record Product(String id, String name, double price, int stock) {
    }

    public record Logistics(String orderId, String company, String trackingNo, String status, String location) {
    }

    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Product> products = new LinkedHashMap<>();
    private final Map<String, Logistics> logistics = new LinkedHashMap<>();

    public MockOrderData() {
        users.put("U1", new User("U1", "张三", "13800000001"));
        users.put("U2", new User("U2", "李四", "13800000002"));

        products.put("P1", new Product("P1", "机械键盘", 399.0, 100));
        products.put("P2", new Product("P2", "27寸显示器", 1299.0, 20));
        products.put("P3", new Product("P3", "鼠标垫", 59.0, 500));

        orders.put("O1001", new Order("O1001", "U1", "P1", "PAID", 399.0));
        orders.put("O1002", new Order("O1002", "U2", "P2", "SHIPPED", 1299.0));
        orders.put("O1003", new Order("O1003", "U1", "P3", "PENDING", 59.0));

        logistics.put("O1002", new Logistics("O1002", "顺丰", "SF123456789", "运输中", "深圳转运中心"));
    }

    public void updateOrderStatus(String orderId, String newStatus) {
        orders.computeIfPresent(orderId,
                (id, order) -> new Order(order.id(), order.userId(), order.productId(), newStatus, order.amount()));
    }

    public Optional<Order> findOrderById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Optional<User> findUserById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    public Optional<Product> findProductById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    public Optional<Logistics> findLogisticsByOrderId(String orderId) {
        return Optional.ofNullable(logistics.get(orderId));
    }
}