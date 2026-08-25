package com.enterprise.agent.supervisor;

import com.enterprise.agent.agent.AgentProperties;
import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderAgentService;
import com.enterprise.agent.agent.OrderTools;
import com.enterprise.agent.rag.RagQaService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Supervisor 端到端联调：真实 DeepSeek 把订单问题分派给订单子助手。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test SupervisorLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class SupervisorLiveTest {

    @Test
    void supervisorRoutesOrderQuestionToOrderAgent() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        // 订单子助手：真实工具
        OrderAgentService orderAgentService = new OrderAgentService(
                model, new OrderTools(new MockOrderData()), new AgentProperties(3));

        // 知识子助手：本用例不触发，用 mock 占位，聚焦验证分派逻辑
        SupervisorTools tools = new SupervisorTools(orderAgentService, mock(RagQaService.class));

        SupervisorAssistant assistant = AiServices.builder(SupervisorAssistant.class)
                .chatModel(model)
                .tools(tools)
                .maxSequentialToolsInvocations(3)
                .build();

        String reply = assistant.chat("查询订单 O1001 的信息");
        System.out.println("[Supervisor 分派回答] " + reply);

        // 399 只可能来自订单子助手，说明总调度员正确分派了任务
        assertThat(reply).contains("O1001");
        assertThat(reply).contains("399");
    }
}