package com.enterprise.agent.observability;

import com.enterprise.agent.agent.OrderAssistant;
import com.enterprise.agent.agent.OrderTools;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.listener.AiServiceResponseReceivedListener;
import dev.langchain4j.service.AiServices;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 带用量采集的订单 Agent（Week 6 Day 4 企业例子）。
 * 通过 AiServiceResponseReceivedListener 累计多轮模型响应的 Token，
 * 最后记录一次「总 Token + 总延迟 + 总成本」。
 */
public class UsageAwareOrderAgentService {

    private final UsageMetricsService metricsService;
    private final OrderAssistant assistant;
    private final AtomicReference<TokenUsage> accumulatedUsage = new AtomicReference<>(new TokenUsage(0, 0, 0));

    public UsageAwareOrderAgentService(OpenAiChatModel chatModel,
                                       OrderTools orderTools,
                                       UsageMetricsService metricsService) {
        this.metricsService = metricsService;
        this.assistant = AiServices.builder(OrderAssistant.class)
                .chatModel(chatModel)
                .tools(orderTools)
                .registerListener(new AiServiceResponseReceivedListener() {
                    @Override
                    public void onEvent(AiServiceResponseReceivedEvent event) {
                        TokenUsage usage = event.response().tokenUsage();
                        if (usage != null) {
                            accumulatedUsage.updateAndGet(current -> current.add(usage));
                        }
                    }
                })
                .maxSequentialToolsInvocations(3)
                .build();
    }

    public String chat(String message) {
        long startNanos = System.nanoTime();
        accumulatedUsage.set(new TokenUsage(0, 0, 0));
        try {
            return assistant.chat(message);
        } finally {
            long durationMillis = (System.nanoTime() - startNanos) / 1_000_000;
            metricsService.record(durationMillis, accumulatedUsage.get());
        }
    }
}
