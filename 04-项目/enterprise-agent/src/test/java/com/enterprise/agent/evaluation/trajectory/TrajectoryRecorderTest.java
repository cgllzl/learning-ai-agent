package com.enterprise.agent.evaluation.trajectory;

import com.enterprise.agent.observability.CostCalculator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TrajectoryRecorderTest {

    @Test
    void keepsSequenceAndDerivesTokenCostAndOperationalSteps() {
        TrajectoryRecorder recorder = new TrajectoryRecorder(new CostCalculator(1.0, 2.0));
        recorder.record("OrderAgent", TrajectoryStepType.AGENT, "start",
                Map.of(), "STARTED", 0, 0, 0);
        recorder.record("OrderAgent", TrajectoryStepType.MODEL, "modelResponse",
                Map.of(), "OK", 20, 100, 50);
        recorder.record("OrderAgent", TrajectoryStepType.TOOL, "getOrder",
                Map.of("arguments", "O1001"), "OK", 5, 0, 0);

        AgentTrajectory trajectory = recorder.finish(
                "CASE", "订单 O1001", true, 30);

        assertThat(trajectory.steps()).extracting(TrajectoryStep::sequence)
                .containsExactly(1, 2, 3);
        assertThat(trajectory.operationalStepCount()).isEqualTo(2);
        assertThat(trajectory.totalTokens()).isEqualTo(150);
        assertThat(trajectory.totalCostUsd()).isEqualTo(0.0002);
    }
}
