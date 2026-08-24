package com.enterprise.agent.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class McpToolProviderTest {

    @Test
    void wiresMcpClientsIntoToolProvider() {
        McpClient client = mock(McpClient.class);

        // McpToolProvider：把 MCP Client 的工具暴露给 AiServices 的标准方式
        McpToolProvider provider = McpToolProvider.builder()
                .mcpClients(List.of(client))
                .build();

        assertThat(provider).isNotNull();
    }
}