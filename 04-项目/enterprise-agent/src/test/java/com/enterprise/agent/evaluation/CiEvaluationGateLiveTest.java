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
 * Week 6 Day 5 的大模型例子：CI 门禁的「真实联调阶段」。
 * 学习例子是订单查询；企业例子是客服回访话术。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test CiEvaluationGateLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class CiEvaluationGateLiveTest {

    private final AgentEvaluationService evaluator = new AgentEvaluationService();

    @Test
    void liveEvaluationGatePasses() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        AgentEvalCase learningCase = AgentEvalCaseCatalog.byId("ORDER_QUERY");
        OrderAgentService orderAgent = new OrderAgentService(
                model, new OrderTools(new MockOrderData()), new AgentProperties(3));
        AgentEvalResult learningResult =
                evaluator.evaluateCorrectness(learningCase, orderAgent.chat(learningCase.input()));
        System.out.println("[CI 评估-学习例子] " + learningResult);
        assertThat(learningResult.passed()).isTrue();

        AgentEvalCase enterpriseCase = AgentEvalCaseCatalog.byId("MULTI_AGENT_MERGE");
        MultiAgentCoordinatorService coordinator = new MultiAgentCoordinatorService(
                new OrderAgentService(model, new OrderTools(new MockOrderData()), new AgentProperties(3)),
                new CustomerReplyService(model));
        AgentEvalResult enterpriseResult = evaluator.evaluateCorrectness(
                enterpriseCase, coordinator.handleCustomerQuestion(enterpriseCase.input()));
        System.out.println("[CI 评估-企业例子] " + enterpriseResult);
        assertThat(enterpriseResult.passed()).isTrue();
    }
}
