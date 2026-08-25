package com.enterprise.agent.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMcpServerTest {

    @Test
    void registersGetOrderAsMcpTool() {
        OrderMcpServer server = new OrderMcpServer();
        assertThat(server.listTools())
                .anyMatch(tool -> "getOrder".equals(tool.name()));
    }

    @Test
    void getOrderReusesExistingBusinessLogic() {
        OrderMcpServer server = new OrderMcpServer();
        assertThat(server.getOrder("O1001"))
                .contains("O1001")
                .contains("399.0");
    }
}