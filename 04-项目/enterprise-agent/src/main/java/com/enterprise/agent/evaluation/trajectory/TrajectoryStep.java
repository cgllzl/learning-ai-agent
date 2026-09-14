package com.enterprise.agent.evaluation.trajectory;

import java.util.Map;

/**
 * Agent 行车记录仪中的一帧：谁做了什么、参数摘要、结果、耗时和 Token。
 */
public record TrajectoryStep(
        int sequence,
        String agentName,
        TrajectoryStepType type,
        String action,
        Map<String, String> parameterSummary,
        String status,
        long durationMillis,
        int inputTokens,
        int outputTokens,
        double costUsd
) {

    public TrajectoryStep {
        parameterSummary = Map.copyOf(parameterSummary);
    }
}
