package com.enterprise.agent.mcp;

import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderTools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryMcpClientTest {

    private final InMemoryMcpClient client =
            new InMemoryMcpClient(new OrderTools(new MockOrderData()));

    @Test
    void listsGetOrderTool() {
        assertThat(client.listTools())
                .anyMatch(tool -> "getOrder".equals(tool.name()));
    }

    @Test
    void executesGetOrderTool() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("getOrder")
                .arguments("{\"orderId\":\"O1001\"}")
                .build();

        ToolExecutionResult result = client.executeTool(request);

        assertThat(result.isError()).isFalse();
        assertThat(result.resultText()).contains("O1001", "399.0");
    }
}