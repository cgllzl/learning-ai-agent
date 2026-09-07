package com.enterprise.agent.workflow.durable;

import com.enterprise.agent.agent.MockOrderData;
import org.springframework.stereotype.Component;

@Component
public class MockOrderWorkflowGateway implements OrderWorkflowGateway {

    private final MockOrderData orderData;

    public MockOrderWorkflowGateway(MockOrderData orderData) {
        this.orderData = orderData;
    }

    @Override
    public String currentStatus(String orderId) {
        return orderData.findOrderById(orderId)
                .map(MockOrderData.Order::status)
                .orElseThrow(() -> new IllegalArgumentException("未找到订单 " + orderId));
    }

    @Override
    public String updateStatus(String orderId, String newStatus) {
        if (orderData.findOrderById(orderId).isEmpty()) {
            throw new IllegalArgumentException("未找到订单 " + orderId);
        }
        orderData.updateOrderStatus(orderId, newStatus);
        return "订单 " + orderId + " 状态已更新为 " + newStatus;
    }
}
