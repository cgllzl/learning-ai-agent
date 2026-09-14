package com.enterprise.agent.evaluation.trajectory;

import com.enterprise.agent.multiagent.CustomerReplyAssistant;
import com.enterprise.agent.observability.CostCalculator;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.listener.AiServiceResponseReceivedListener;
import dev.langchain4j.service.AiServices;

import java.util.Map;

/**
 * 订单 Agent → 客服回复 Agent 的完整轨迹记录器。
 */
public class TrajectoryRecordingMultiAgentService {

    private final CostCalculator costCalculator;
    private final TrajectoryRecordingOrderAgentService orderAgent;
    private final CustomerReplyAssistant replyAssistant;
    private final ThreadLocal<TrajectoryRecorder> activeRecorder = new ThreadLocal<>();

    public TrajectoryRecordingMultiAgentService(OpenAiChatModel chatModel,
                                                TrajectoryRecordingOrderAgentService orderAgent,
                                                CostCalculator costCalculator) {
        this.costCalculator = costCalculator;
        this.orderAgent = orderAgent;
        this.replyAssistant = AiServices.builder(CustomerReplyAssistant.class)
                .chatModel(chatModel)
                .registerListener(new AiServiceResponseReceivedListener() {
                    @Override
                    public void onEvent(AiServiceResponseReceivedEvent event) {
                        TrajectoryRecorder recorder = activeRecorder.get();
                        if (recorder != null) {
                            recorder.recordModelResponse(
                                    "CustomerReplyAgent", event.response().tokenUsage());
                        }
                    }
                })
                .build();
    }

    public AgentTrajectory run(String caseId, String question) {
        TrajectoryRecorder recorder = new TrajectoryRecorder(costCalculator);
        long started = System.nanoTime();
        recorder.record("MultiAgentCoordinator", TrajectoryStepType.AGENT,
                "multiAgentStart", Map.of(), "STARTED", 0, 0, 0);

        String orderFacts = orderAgent.executeInto(question, recorder);
        recorder.record("MultiAgentCoordinator", TrajectoryStepType.HANDOFF,
                "orderToCustomerReply", Map.of("payloadType", "orderFacts"),
                "OK", 0, 0, 0);

        recorder.record("CustomerReplyAgent", TrajectoryStepType.AGENT,
                "customerReplyStart", Map.of(), "STARTED", 0, 0, 0);
        activeRecorder.set(recorder);
        String finalAnswer;
        try {
            finalAnswer = replyAssistant.compose(question, orderFacts);
            recorder.record("CustomerReplyAgent", TrajectoryStepType.AGENT,
                    "customerReplyEnd", Map.of(), "OK", 0, 0, 0);
        } catch (RuntimeException e) {
            recorder.record("CustomerReplyAgent", TrajectoryStepType.AGENT,
                    "customerReplyEnd", Map.of("errorType", e.getClass().getSimpleName()),
                    "ERROR", 0, 0, 0);
            throw e;
        } finally {
            activeRecorder.remove();
        }

        recorder.record("MultiAgentCoordinator", TrajectoryStepType.AGENT,
                "multiAgentEnd", Map.of(), "OK", 0, 0, 0);
        long durationMillis = (System.nanoTime() - started) / 1_000_000;
        return recorder.finish(caseId, finalAnswer, true, durationMillis);
    }
}
