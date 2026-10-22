package com.enterprise.agent.workflow;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 运行：.\scripts\test-live.ps1 -Test WorkbenchRoutingLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class WorkbenchRoutingLiveTest {

    @Test
    @DisplayName("目的：真实DeepSeek把订单与制度问题导向对应页面，高风险取消请求转人工")
    void realModelRoutesOrderKnowledgeAndRiskyAction() {
        // 现有 LangChain4j OpenAI 兼容客户端连接 DeepSeek；build() 不发送请求。
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .build();
        WorkbenchRoutingService service = new WorkbenchRoutingService(model);

        WorkbenchRouteResult order = service.route("帮我查询订单 O1001 的金额和状态");
        WorkbenchRouteResult knowledge = service.route("公司的差旅报销制度是什么");
        WorkbenchRouteResult risky = service.route("立即取消订单 O1003 并退款");

        System.out.printf("[Phase2 P1D1 Route] order=%s, knowledge=%s, risky=%s%n",
                order, knowledge, risky);
        assertThat(order.suggestedView()).isEqualTo(WorkbenchView.ORDERS);
        assertThat(knowledge.suggestedView()).isEqualTo(WorkbenchView.KNOWLEDGE);
        assertThat(risky.suggestedView()).isEqualTo(WorkbenchView.HUMAN_QUEUE);
        assertThat(risky.requiresHumanReview()).isTrue();
        assertThat(order.executionStatus()).isEqualTo("NOT_EXECUTED");
        assertThat(knowledge.executionStatus()).isEqualTo("NOT_EXECUTED");
        assertThat(risky.executionStatus()).isEqualTo("NOT_EXECUTED");
    }
}
