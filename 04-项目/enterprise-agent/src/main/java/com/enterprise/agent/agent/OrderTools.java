package com.enterprise.agent.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 企业订单 Agent 的工具集（Day 5：查询订单/用户/商品/物流 + 修改订单状态）。
 */
@Component
public class OrderTools {

    private static final Set<String> VALID_STATUSES = Set.of("PAID", "SHIPPED", "DELIVERED", "CANCELLED");

    private final MockOrderData data;

    public OrderTools(MockOrderData data) {
        this.data = data;
    }

    @Tool("根据订单号查询订单信息")
    public String getOrder(@P("订单号，例如 O1001") String orderId) {
        return data.findOrderById(orderId)
                .map(order -> "订单 " + order.id()
                        + "：用户 " + order.userId()
                        + "，商品 " + order.productId()
                        + "，金额 " + order.amount() + " 元，状态 " + order.status())
                .orElse("未找到订单 " + orderId);
    }

    @Tool("根据用户 ID 查询用户信息")
    public String getUser(@P("用户 ID，例如 U1") String userId) {
        return data.findUserById(userId)
                .map(user -> "用户 " + user.id() + "：" + user.name() + "，电话 " + user.phone())
                .orElse("未找到用户 " + userId);
    }

    @Tool("根据商品 ID 查询商品信息")
    public String getProduct(@P("商品 ID，例如 P1") String productId) {
        return data.findProductById(productId)
                .map(product -> "商品 " + product.id() + "：" + product.name()
                        + "，价格 " + product.price() + " 元，库存 " + product.stock())
                .orElse("未找到商品 " + productId);
    }

    @Tool("根据订单号查询物流信息")
    public String getLogistics(@P("订单号，例如 O1002") String orderId) {
        return data.findLogisticsByOrderId(orderId)
                .map(logistics -> "订单 " + logistics.orderId() + " 物流：" + logistics.company()
                        + "，运单号 " + logistics.trackingNo()
                        + "，状态 " + logistics.status() + "，" + logistics.location())
                .orElse("未找到订单 " + orderId + " 的物流信息");
    }

    @Tool("修改订单状态。只有状态为 PENDING 的订单可以被修改；新状态必须是 PAID、SHIPPED、DELIVERED 或 CANCELLED 之一")
    public String updateOrderStatus(@P("订单号") String orderId,
                                    @P("新状态，可选 PAID/SHIPPED/DELIVERED/CANCELLED") String newStatus) {
        if (!VALID_STATUSES.contains(newStatus)) {
            return "非法状态 " + newStatus + "，允许值：PAID/SHIPPED/DELIVERED/CANCELLED";
        }
        MockOrderData.Order order = data.findOrderById(orderId).orElse(null);
        if (order == null) {
            return "未找到订单 " + orderId;
        }
        if (!"PENDING".equals(order.status())) {
            return "订单 " + orderId + " 当前状态为 " + order.status() + "，只有 PENDING 状态可以修改";
        }
        data.updateOrderStatus(orderId, newStatus);
        return "订单 " + orderId + " 状态已更新为 " + newStatus;
    }
}