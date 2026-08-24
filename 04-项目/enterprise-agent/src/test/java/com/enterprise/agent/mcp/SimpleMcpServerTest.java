package com.enterprise.agent.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleMcpServerTest {

    @Test
    void serverRegistersAddTool() {
        SimpleMcpServer server = new SimpleMcpServer();
        assertThat(server.listTools())
                .anyMatch(tool -> "add".equals(tool.name()));
    }

    @Test
    void addComputesSum() {
        SimpleMcpServer server = new SimpleMcpServer();
        assertThat(server.add(2, 3)).isEqualTo(5);
    }
}