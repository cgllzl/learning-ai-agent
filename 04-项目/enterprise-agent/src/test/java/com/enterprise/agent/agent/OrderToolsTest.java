package com.enterprise.agent.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderToolsTest {

    private OrderTools tools;

    @BeforeEach
    void setUp() {
        tools = new OrderTools(new MockOrderData());
    }

    @Test
    void getOrderReturnsOrderDetails() {
        String result = tools.getOrder("O1001");
        assertThat(result).contains("O1001", "张三", "机械键盘", "399.0", "PAID");
    }

    @Test
    void getOrderReturnsNotFoundForUnknownOrder() {
        assertThat(tools.getOrder("O9999")).contains("未找到订单");
    }

    @Test
    void getUserAndGetProductWork() {
        assertThat(tools.getUser("U1")).contains("张三");
        assertThat(tools.getProduct("P1")).contains("机械键盘", "399.0");
    }

    @Test
    void getLogisticsReturnsTrackingInfo() {
        assertThat(tools.getLogistics("O1002")).contains("顺丰", "SF123456789");
    }

    @Test
    void updateStatusSucceedsForPendingOrder() {
        String result = tools.updateOrderStatus("O1003", "SHIPPED");
        assertThat(result).contains("已更新为 SHIPPED");
    }

    @Test
    void updateStatusRejectsInvalidStatus() {
        assertThat(tools.updateOrderStatus("O1003", "XXX")).contains("非法状态");
    }

    @Test
    void updateStatusRejectsNonPendingOrder() {
        assertThat(tools.updateOrderStatus("O1001", "SHIPPED")).contains("只有 PENDING 状态可以修改");
    }

    @Test
    void updateStatusRejectsUnknownOrder() {
        assertThat(tools.updateOrderStatus("O9999", "PAID")).contains("未找到订单");
    }
}