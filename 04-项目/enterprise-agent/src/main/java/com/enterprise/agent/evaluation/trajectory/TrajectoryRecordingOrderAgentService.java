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
 * 自动记录模型响应与真实 Tool 调用的订单 Agent。
 * 记录动作来自框架回调，不会为了采集指标而把同一个业务请求再执行一遍。
 */
public class TrajectoryRecordingOrderAgentService {

    private final CostCalculator costCalculator;
    private final OrderAssistant assistant;
    // 框架回调没有 recorder 参数，因此用 ThreadLocal 把“当前线程的回调”归入本次 run。
    // 它不会自动跨异步线程传播；若以后改为异步执行，应换成显式的 run-scoped context。
    private final ThreadLocal<TrajectoryRecorder> activeRecorder = new ThreadLocal<>();

    public TrajectoryRecordingOrderAgentService(OpenAiChatModel chatModel,
                                                OrderTools orderTools,
                                                CostCalculator costCalculator) {
        this.costCalculator = costCalculator;
        this.assistant = AiServices.builder(OrderAssistant.class)
                .chatModel(chatModel)
                .tools(orderTools)
                .afterToolExecution(tool -> {
                    // Tool 真正执行后再记录名称、参数摘要、耗时和成败，避免把“计划调用”误当成“已经调用”。
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
                        // Token 来自真实模型响应；成本是 Token × 配置单价的估算值，不是供应商账单。
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
        // 查询场景暂把正常返回视为完成；生产写操作应再查数据库或业务 API 验证真实结果。
        return recorder.finish(caseId, answer, true, durationMillis);
    }

    /**
     * 把订单 Agent 的步骤写入调用方提供的 recorder，使 Multi-Agent 能复用同一条端到端轨迹。
     */
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
            // 清除线程绑定，避免线程池长期持有已结束的 recorder，也防止未重新绑定的回调误写旧轨迹。
            activeRecorder.remove();
        }
    }

    private String abbreviate(String text) {
        // 截断只控制轨迹体积，不等于脱敏；生产环境还必须移除密钥和个人信息。
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }
}
