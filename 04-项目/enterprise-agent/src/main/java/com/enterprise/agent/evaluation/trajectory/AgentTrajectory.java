package com.enterprise.agent.evaluation.trajectory;

import java.util.List;

public record AgentTrajectory(
        String runId,
        String caseId,
        String answer,
        boolean taskCompleted,
        long durationMillis,
        List<TrajectoryStep> steps
) {

    public AgentTrajectory {
        steps = List.copyOf(steps);
    }

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
