package com.enterprise.agent.evaluation.trajectory;

public record AgentRunSample(
        String approach,
        String caseId,
        boolean passed,
        long latencyMillis,
        double costUsd,
        int stepCount,
        int totalTokens
) {

    public static AgentRunSample from(String approach,
                                      AgentTrajectory trajectory,
                                      TrajectoryEvaluationResult result) {
        return new AgentRunSample(
                approach,
                trajectory.caseId(),
                result.passed(),
                trajectory.durationMillis(),
                trajectory.totalCostUsd(),
                trajectory.operationalStepCount(),
                trajectory.totalTokens()
        );
    }
}
