package com.enterprise.agent.multiagent;

import com.enterprise.agent.agent.AgentProperties;
import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderAgentService;
import com.enterprise.agent.agent.OrderTools;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多 Agent 状态传递 + 结果合并的端到端联调（真实 DeepSeek）。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test MultiAgentLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class MultiAgentLiveTest {

    @Test
    void orderAgentStateIsMergedByReplyAgent() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        // Agent A：订单查询（真实工具 + 大模型）
        OrderAgentService orderAgentService = new OrderAgentService(
                model, new OrderTools(new MockOrderData()), new AgentProperties(3));

        // Agent B：客服回复（大模型合并状态）
        CustomerReplyService customerReplyService = new CustomerReplyService(model);

        MultiAgentCoordinatorService coordinator =
                new MultiAgentCoordinatorService(orderAgentService, customerReplyService);

        String reply = coordinator.handleCustomerQuestion(
                "查询订单 O1001 的信息，并生成一段客服回访话术");
        System.out.println("[MultiAgent 状态传递回答] " + reply);

        // O1001 和 399 最初由订单工具返回，最终仍出现在回复里，
        // 说明订单 Agent 的状态被正确传给了客服回复 Agent 并完成合并。
        assertThat(reply).contains("O1001");
        assertThat(reply).contains("399");
    }
}
