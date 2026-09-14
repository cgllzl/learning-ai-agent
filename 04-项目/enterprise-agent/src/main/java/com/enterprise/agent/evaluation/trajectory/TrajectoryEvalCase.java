package com.enterprise.agent.evaluation.trajectory;

public record TrajectoryEvalCase(
        String id,
        EvaluationSplit split,
        String input,
        TrajectoryExpectation expectation
) {
}
