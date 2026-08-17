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
        assertThat(result).contains("O1001", "U1", "399.0", "PAID");
    }

    @Test
    void getOrderReturnsNotFoundForUnknownOrder() {
        assertThat(tools.getOrder("O9999")).contains("未找到订单");
    }

    @Test
    void getUserReturnsUserDetails() {
        assertThat(tools.getUser("U1")).contains("U1", "张三");
    }

    @Test
    void getUserReturnsNotFoundForUnknownUser() {
        assertThat(tools.getUser("U999")).contains("未找到用户");
    }

    @Test
    void getProductReturnsProductDetails() {
        assertThat(tools.getProduct("P1")).contains("P1", "机械键盘", "399.0");
    }

    @Test
    void getProductReturnsNotFoundForUnknownProduct() {
        assertThat(tools.getProduct("P999")).contains("未找到商品");
    }

    @Test
    void getLogisticsReturnsTrackingInfo() {
        assertThat(tools.getLogistics("O1002")).contains("顺丰", "SF123456789");
    }

    @Test
    void getLogisticsReturnsNotFoundForUnknownOrder() {
        assertThat(tools.getLogistics("O1001")).contains("未找到订单");
    }
}