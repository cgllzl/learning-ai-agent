package com.enterprise.agent.mcp;

import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderTools;
import com.enterprise.agent.chat.DeepSeekProperties;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端联调：大模型通过 McpToolProvider 真正调用 MCP 工具。
 * 需设置 DEEPSEEK_API_KEY：
 *   $env:DEEPSEEK_API_KEY="..." ; mvn test -Dtest=McpOrderLiveTest -Dsurefire.useFile=false
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class McpOrderLiveTest {

    @Test
    void llmCallsMcpTool() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        // MCP 工具桥接进 Agent
        McpToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(new InMemoryMcpClient(new OrderTools(new MockOrderData())))
                .build();

        McpOrderAssistant assistant = AiServices.builder(McpOrderAssistant.class)
                .chatModel(model)
                .toolProvider(toolProvider)
                .build();

        String reply = assistant.chat("查询订单 O1001 的信息");
        System.out.println("[MCP 联动回答] " + reply);

        // 金额只可能来自 MCP 工具返回，说明模型真的通过 MCP 调用了 getOrder
        assertThat(reply).contains("O1001");
        assertThat(reply).contains("399");
    }
}