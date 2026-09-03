package com.enterprise.agent.evaluation;

import com.enterprise.agent.agent.AgentProperties;
import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderAgentService;
import com.enterprise.agent.agent.OrderTools;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Week 6 Day 6 的大模型例子：企业订单查询用例先模拟失败，再换真实 Agent 改进后通过。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test FailureReviewLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class FailureReviewLiveTest {

    private final EvalRegressionRunner runner =
            new EvalRegressionRunner(new AgentEvaluationService());

    @Test
    void reviewsWeakAnswerThenFixesWithRealAgent() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        AgentEvalCase orderCase = AgentEvalCaseCatalog.byId("ORDER_QUERY");

        // 1) 先制造一个缺少金额的弱回答，复现失败
        EvalRunReport failedReport = runner.run(
                List.of(orderCase), ignored -> "订单 O1001 的信息");
        System.out.println("[复盘-失败报告] " + failedReport);
        assertThat(failedReport.failed()).isEqualTo(1);

        // 2) 复盘后改用真实订单 Agent 回答
        OrderAgentService orderAgent = new OrderAgentService(
                model, new OrderTools(new MockOrderData()), new AgentProperties(3));
        EvalRunReport improvedReport = runner.run(
                List.of(orderCase), evalCase -> orderAgent.chat(evalCase.input()));
        System.out.println("[复盘-改进报告] " + improvedReport);

        assertThat(improvedReport.passed()).isEqualTo(1);
        assertThat(improvedReport.success()).isTrue();
    }
}
