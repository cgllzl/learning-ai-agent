package com.enterprise.agent.observability;

import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderTools;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Week 6 Day 3 的大模型例子：企业客服查订单场景，整次对话 + 工具调用都可追踪。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test TraceLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class TraceLiveTest {

    @Test
    void tracesWholeConversationAndToolCall() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        AgentTracer tracer = new AgentTracer();
        TraceableOrderAgentService agent = new TraceableOrderAgentService(
                model, new OrderTools(new MockOrderData()), tracer);

        String reply = agent.chat("查询订单 O1001 的信息");
        System.out.println("[Trace 对话回答] " + reply);
        assertThat(reply).contains("O1001").contains("399");

        System.out.println("[Trace Spans]");
        tracer.spans().forEach(span ->
                System.out.println(span.spanId() + " <- " + span.parentSpanId()
                        + " | " + span.name() + " | " + span.status()
                        + " | " + span.durationNanos() / 1_000_000 + "ms"));

        assertThat(tracer.spans())
                .extracting(TraceSpan::name)
                .contains("AGENT:chat", "TOOL:getOrder");

        TraceSpan root = tracer.spans().stream()
                .filter(span -> span.name().equals("AGENT:chat"))
                .findFirst().orElseThrow();
        TraceSpan tool = tracer.spans().stream()
                .filter(span -> span.name().equals("TOOL:getOrder"))
                .findFirst().orElseThrow();
        assertThat(tool.parentSpanId()).isEqualTo(root.spanId());
    }
}
