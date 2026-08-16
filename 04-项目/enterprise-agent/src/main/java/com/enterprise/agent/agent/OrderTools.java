package com.enterprise.agent.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 企业订单 Agent 的工具集（Day 2：先实现第一个工具 getOrder）。
 */
@Component
public class OrderTools {

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
}