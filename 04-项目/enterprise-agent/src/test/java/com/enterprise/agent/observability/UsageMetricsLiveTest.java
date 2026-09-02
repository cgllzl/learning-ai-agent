package com.enterprise.agent.observability;

import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderTools;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Week 6 Day 4 的大模型例子：真实调用 DeepSeek，记录 Token 用量、成本与延迟。
 * 企业例子模拟客服批量回复，用来观察 API 成本。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test UsageMetricsLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class UsageMetricsLiveTest {

    @Test
    void recordsTokenUsageCostAndLatencyForCustomerServiceReplies() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        UsageMetricsService metricsService =
                new UsageMetricsService(new CostCalculator(0.27, 1.10));
        UsageAwareChatService chatService =
                new UsageAwareChatService(model, metricsService);

        // 学习例子：普通聊天
        String learningReply = chatService.chat("你是友好助手", "请用一句话介绍你自己");
        System.out.println("[学习例子回答] " + learningReply);

        // 企业例子：客服查询订单（会真实调用 getOrder 工具）
        UsageAwareOrderAgentService orderAgentService =
                new UsageAwareOrderAgentService(model, new OrderTools(new MockOrderData()), metricsService);
        String enterpriseReply = orderAgentService.chat("查询订单 O1001 的信息");
        System.out.println("[企业例子回答] " + enterpriseReply);
        assertThat(enterpriseReply).contains("O1001").contains("399");

        UsageSummary summary = metricsService.summary();
        System.out.println("[用量汇总] " + summary);

        assertThat(summary.totalRequests()).isEqualTo(2);
        assertThat(summary.totalTokens()).isGreaterThan(0);
        assertThat(summary.totalCostUsd()).isGreaterThan(0.0);
        assertThat(summary.averageDurationMillis()).isGreaterThanOrEqualTo(0.0);
    }
}
