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
 * 两个 Agent 共享同一个 recorder，才能按一次用户请求汇总 Tool、Token、Handoff 和总成本。
 */
public class TrajectoryRecordingMultiAgentService {

    private final CostCalculator costCalculator;
    private final TrajectoryRecordingOrderAgentService orderAgent;
    private final CustomerReplyAssistant replyAssistant;
    // 用途与订单 Agent 中相同：只关联当前线程；异步跨线程时需要显式传递运行上下文。
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

        // 复用同一个 recorder，订单 Agent 的模型和 Tool 步骤才会留在这条端到端轨迹里。
        String orderFacts = orderAgent.executeInto(question, recorder);
        // Handoff 是本编排器显式记录的交接点，并非框架自动推断；只记载荷类型，不落原始业务数据。
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
            // 清除线程绑定，避免线程池残留已结束 run 的 recorder。
            activeRecorder.remove();
        }

        recorder.record("MultiAgentCoordinator", TrajectoryStepType.AGENT,
                "multiAgentEnd", Map.of(), "OK", 0, 0, 0);
        // 顶层计时覆盖整个串行流程；不能简单把步骤耗时相加，以免未来并行步骤被重复计算。
        long durationMillis = (System.nanoTime() - started) / 1_000_000;
        // 查询场景暂把正常返回视为完成；生产写操作应独立核验业务状态。
        return recorder.finish(caseId, finalAnswer, true, durationMillis);
    }
}
