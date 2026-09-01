package com.enterprise.agent.observability;

import com.enterprise.agent.agent.OrderAssistant;
import com.enterprise.agent.agent.OrderTools;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import java.util.Map;

/**
 * 可追踪的订单 Agent（Week 6 Day 3）。
 * 根 Span 包住整次对话，模型每次调工具都会产生一个 Tool Span。
 */
public class TraceableOrderAgentService {

    private final AgentTracer tracer;
    private final OrderAssistant assistant;

    public TraceableOrderAgentService(OpenAiChatModel chatModel,
                                      OrderTools orderTools,
                                      AgentTracer tracer) {
        this.tracer = tracer;
        this.assistant = AiServices.builder(OrderAssistant.class)
                .chatModel(chatModel)
                .tools(orderTools)
                .beforeToolExecution(before -> tracer.startSpan(
                        "TOOL:" + before.request().name(),
                        Map.of("arguments", before.request().arguments())))
                .afterToolExecution(after -> tracer.endSpan(
                        after.hasFailed() ? "ERROR" : "OK",
                        Map.of(
                                "result", String.valueOf(after.result()),
                                "duration_ms", after.duration().toMillis() + "ms")))
                .maxSequentialToolsInvocations(3)
                .build();
    }

    public String chat(String message) {
        tracer.startSpan("AGENT:chat", Map.of("input", message));
        try {
            return assistant.chat(message);
        } finally {
            tracer.endSpan("OK", Map.of());
        }
    }
}
