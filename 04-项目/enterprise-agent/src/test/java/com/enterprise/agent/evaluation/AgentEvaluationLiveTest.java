package com.enterprise.agent.evaluation;

import com.enterprise.agent.agent.AgentProperties;
import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderAgentService;
import com.enterprise.agent.agent.OrderTools;
import com.enterprise.agent.multiagent.CustomerReplyService;
import com.enterprise.agent.multiagent.MultiAgentCoordinatorService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Week 6 Day 2 的大模型例子：自动化评估真实运行两条用例。
 * 学习例子是订单查询正确性，企业例子是客服回访话术端到端正确性。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test AgentEvaluationLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class AgentEvaluationLiveTest {

    private final AgentEvaluationService evaluator = new AgentEvaluationService();

    @Test
    void runsCorrectnessEvaluationForLearningAndEnterpriseCases() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        // 学习例子：订单查询正确性
        AgentEvalCase orderCase = AgentEvalCaseCatalog.byId("ORDER_QUERY");
        OrderAgentService orderAgent = new OrderAgentService(
                model, new OrderTools(new MockOrderData()), new AgentProperties(3));
        AgentEvalResult orderResult =
                evaluator.evaluateCorrectness(orderCase, orderAgent.chat(orderCase.input()));
        System.out.println("[评估-订单查询] " + orderResult);
        assertThat(orderResult.passed()).isTrue();

        // 企业例子：客服回访话术（多 Agent 状态传递）
        AgentEvalCase enterpriseCase = AgentEvalCaseCatalog.byId("MULTI_AGENT_MERGE");
        MultiAgentCoordinatorService coordinator = new MultiAgentCoordinatorService(
                new OrderAgentService(model, new OrderTools(new MockOrderData()), new AgentProperties(3)),
                new CustomerReplyService(model));
        AgentEvalResult enterpriseResult = evaluator.evaluateCorrectness(
                enterpriseCase, coordinator.handleCustomerQuestion(enterpriseCase.input()));
        System.out.println("[评估-客服回访] " + enterpriseResult);
        assertThat(enterpriseResult.passed()).isTrue();
    }
}
