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
}