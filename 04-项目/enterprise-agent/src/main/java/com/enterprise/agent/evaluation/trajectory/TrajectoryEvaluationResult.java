package com.enterprise.agent.evaluation.trajectory;

import java.util.List;

public record TrajectoryEvaluationResult(
        boolean answerPassed,
        boolean outcomePassed,
        boolean trajectoryPassed,
        boolean passed,
        List<String> violations
) {

    public TrajectoryEvaluationResult {
        violations = List.copyOf(violations);
    }
}
