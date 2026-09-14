package com.enterprise.agent.evaluation.trajectory;

import com.enterprise.agent.observability.CostCalculator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证记录顺序以及从原始步骤汇总操作数、Token 和估算成本的规则。 */
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
        // AGENT 是生命周期标记，因此实际操作只有 MODEL 和 TOOL 两步。
        assertThat(trajectory.operationalStepCount()).isEqualTo(2);
        assertThat(trajectory.totalTokens()).isEqualTo(150);
        // (100 输入 × $1 + 50 输出 × $2) / 1,000,000 = $0.0002。
        assertThat(trajectory.totalCostUsd()).isEqualTo(0.0002);
    }
}
