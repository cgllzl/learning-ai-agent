package com.enterprise.agent.evaluation.trajectory;

import com.enterprise.agent.agent.OrderAssistant;
import com.enterprise.agent.agent.OrderTools;
import com.enterprise.agent.observability.CostCalculator;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.listener.AiServiceResponseReceivedListener;
import dev.langchain4j.service.AiServices;

import java.util.Map;

/**
 * 自动记录模型响应与 Tool 调用的订单 Agent。
 */
public class TrajectoryRecordingOrderAgentService {

    private final CostCalculator costCalculator;
    private final OrderAssistant assistant;
    private final ThreadLocal<TrajectoryRecorder> activeRecorder = new ThreadLocal<>();

    public TrajectoryRecordingOrderAgentService(OpenAiChatModel chatModel,
                                                OrderTools orderTools,
                                                CostCalculator costCalculator) {
        this.costCalculator = costCalculator;
        this.assistant = AiServices.builder(OrderAssistant.class)
                .chatModel(chatModel)
                .tools(orderTools)
                .afterToolExecution(tool -> {
                    TrajectoryRecorder recorder = activeRecorder.get();
                    if (recorder != null) {
                        recorder.record(
                                "OrderAgent",
                                TrajectoryStepType.TOOL,
                                tool.request().name(),
                                Map.of("arguments", abbreviate(String.valueOf(tool.request().arguments()))),
                                tool.hasFailed() ? "ERROR" : "OK",
                                tool.duration().toMillis(),
                                0,
                                0
                        );
                    }
                })
                .registerListener(new AiServiceResponseReceivedListener() {
                    @Override
                    public void onEvent(AiServiceResponseReceivedEvent event) {
                        TrajectoryRecorder recorder = activeRecorder.get();
                        if (recorder != null) {
                            recorder.recordModelResponse("OrderAgent", event.response().tokenUsage());
                        }
                    }
                })
                .maxSequentialToolsInvocations(3)
                .build();
    }

    public AgentTrajectory run(String caseId, String message) {
        TrajectoryRecorder recorder = new TrajectoryRecorder(costCalculator);
        long started = System.nanoTime();
        String answer = executeInto(message, recorder);
        long durationMillis = (System.nanoTime() - started) / 1_000_000;
        return recorder.finish(caseId, answer, true, durationMillis);
    }

    String executeInto(String message, TrajectoryRecorder recorder) {
        recorder.record("OrderAgent", TrajectoryStepType.AGENT, "orderAgentStart",
                Map.of(), "STARTED", 0, 0, 0);
        activeRecorder.set(recorder);
        try {
            String answer = assistant.chat(message);
            recorder.record("OrderAgent", TrajectoryStepType.AGENT, "orderAgentEnd",
                    Map.of(), "OK", 0, 0, 0);
            return answer;
        } catch (RuntimeException e) {
            recorder.record("OrderAgent", TrajectoryStepType.AGENT, "orderAgentEnd",
                    Map.of("errorType", e.getClass().getSimpleName()), "ERROR", 0, 0, 0);
            throw e;
        } finally {
            activeRecorder.remove();
        }
    }

    private String abbreviate(String text) {
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }
}
