package com.enterprise.agent.evaluation.trajectory;

import com.enterprise.agent.observability.CostCalculator;
import dev.langchain4j.model.output.TokenUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 当前一次运行的线程内轨迹记录器。
 */
public class TrajectoryRecorder {

    private final String runId = UUID.randomUUID().toString();
    private final AtomicInteger sequence = new AtomicInteger();
    private final List<TrajectoryStep> steps = new ArrayList<>();
    private final CostCalculator costCalculator;

    public TrajectoryRecorder(CostCalculator costCalculator) {
        this.costCalculator = costCalculator;
    }

    public synchronized void record(String agentName,
                                    TrajectoryStepType type,
                                    String action,
                                    Map<String, String> parameterSummary,
                                    String status,
                                    long durationMillis,
                                    int inputTokens,
                                    int outputTokens) {
        steps.add(new TrajectoryStep(
                sequence.incrementAndGet(),
                agentName,
                type,
                action,
                parameterSummary,
                status,
                durationMillis,
                inputTokens,
                outputTokens,
                costCalculator.calculateUsd(inputTokens, outputTokens)
        ));
    }

    public void recordModelResponse(String agentName, TokenUsage usage) {
        int inputTokens = usage == null || usage.inputTokenCount() == null ? 0 : usage.inputTokenCount();
        int outputTokens = usage == null || usage.outputTokenCount() == null ? 0 : usage.outputTokenCount();
        record(agentName, TrajectoryStepType.MODEL, "modelResponse", Map.of(),
                "OK", 0, inputTokens, outputTokens);
    }

    public AgentTrajectory finish(String caseId,
                                  String answer,
                                  boolean taskCompleted,
                                  long durationMillis) {
        return new AgentTrajectory(
                runId, caseId, answer, taskCompleted, durationMillis, List.copyOf(steps));
    }
}
