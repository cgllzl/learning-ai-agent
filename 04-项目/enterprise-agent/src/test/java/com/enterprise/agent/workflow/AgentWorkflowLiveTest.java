package com.enterprise.agent.workflow;

import com.enterprise.agent.agent.AgentProperties;
import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderAgentService;
import com.enterprise.agent.agent.OrderTools;
import com.enterprise.agent.rag.RagQaService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Week 6.5 Day 1 真实联调：DeepSeek 分类、订单 Agent 调用工具、DeepSeek 质量复核。
 *
 * 运行：.\scripts\test-live.ps1 -Test AgentWorkflowLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class AgentWorkflowLiveTest {

    @Test
    void realModelRoutesOrderQuestionAndFiniteLoopCompletes() {
        OpenAiChatModel model = model();
        OrderAgentService orderAgent = new OrderAgentService(
                model,
                new OrderTools(new MockOrderData()),
                new AgentProperties(3)
        );
        RagQaService ragQa = mock(RagQaService.class);
        TicketWorkflowService workflow = new TicketWorkflowService(
                model,
                orderAgent,
                ragQa,
                new AgentWorkflowProperties(3)
        );

        TicketWorkflowResult result = workflow.handle("查询订单 O1001 的金额和状态");
        System.out.println("[企业工单 Workflow] " + result);

        assertThat(result.category()).isEqualTo(TicketCategory.ORDER);
        assertThat(result.status()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(result.reply()).contains("O1001").contains("399").containsIgnoringCase("PAID");
        assertThat(result.reviewIterations()).isBetween(1, 3);
        verify(ragQa, never()).ask(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void realModelRoutesHighRiskActionToHumanWithoutCallingBusinessBranch() {
        OpenAiChatModel model = model();
        OrderAgentService orderAgent = mock(OrderAgentService.class);
        RagQaService ragQa = mock(RagQaService.class);
        TicketWorkflowService workflow = new TicketWorkflowService(
                model,
                orderAgent,
                ragQa,
                new AgentWorkflowProperties(3)
        );

        TicketWorkflowResult result = workflow.handle("立即替我取消订单并退款，我没有提供订单号");
        System.out.println("[高风险工单路由] " + result);

        assertThat(result.category()).isEqualTo(TicketCategory.HUMAN);
        assertThat(result.status()).isEqualTo(WorkflowStatus.HUMAN_REQUIRED);
        verify(orderAgent, never()).chat(org.mockito.ArgumentMatchers.anyString());
        verify(ragQa, never()).ask(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    private OpenAiChatModel model() {
        return OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
