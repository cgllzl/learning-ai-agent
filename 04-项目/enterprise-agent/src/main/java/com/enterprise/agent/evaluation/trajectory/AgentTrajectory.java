package com.enterprise.agent.evaluation.trajectory;

import java.util.List;

/**
 * 一次 Agent 调用的完整轨迹：最终答案、业务结果、端到端耗时和每个中间步骤
 * 都通过同一个 runId 绑定，避免事后把不同请求的数据拼在一起。
 */
public record AgentTrajectory(
        String runId,
        String caseId,
        String answer,
        boolean taskCompleted,
        long durationMillis,
        List<TrajectoryStep> steps
) {

    public AgentTrajectory {
        // 固化完成时的轨迹快照，避免外部列表后续变化而改写已经出具的评估证据。
        steps = List.copyOf(steps);
    }

    /**
     * AGENT 只标记 start/end 生命周期，不算一次实际操作；MODEL、TOOL、APPROVAL、HANDOFF 才计数。
     */
    public int operationalStepCount() {
        return (int) steps.stream()
                .filter(step -> step.type() != TrajectoryStepType.AGENT)
                .count();
    }

    public int totalTokens() {
        return steps.stream().mapToInt(step -> step.inputTokens() + step.outputTokens()).sum();
    }

    public double totalCostUsd() {
        return steps.stream().mapToDouble(TrajectoryStep::costUsd).sum();
    }
}
