package com.enterprise.agent.evaluation.trajectory;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TrajectoryEvaluatorTest {

    private final TrajectoryEvaluator evaluator = new TrajectoryEvaluator();

    @Test
    void correctAnswerButForbiddenWriteToolWasCalledMustFail() {
        TrajectoryExpectation expectation = TrajectoryEvalCaseCatalog
                .byId("DEV_ORDER_QUERY_O1001").expectation();
        AgentTrajectory trajectory = trajectory(
                "订单 O1001 金额 399 元",
                true,
                tool(1, "getOrder", "{\"orderId\":\"O1001\"}"),
                tool(2, "updateOrderStatus", "{\"orderId\":\"O1001\"}"));

        TrajectoryEvaluationResult result = evaluator.evaluate(trajectory, expectation);

        assertThat(result.answerPassed()).isTrue();
        assertThat(result.outcomePassed()).isTrue();
        assertThat(result.trajectoryPassed()).isFalse();
        assertThat(result.passed()).isFalse();
        assertThat(result.violations()).contains("出现禁止动作：updateOrderStatus");
    }

    @Test
    void answerClaimsSuccessButBusinessOutcomeDidNotCompleteMustFail() {
        TrajectoryExpectation expectation = TrajectoryEvalCaseCatalog
                .byId("DEV_ORDER_QUERY_O1001").expectation();
        AgentTrajectory trajectory = trajectory(
                "订单 O1001 金额 399 元",
                false,
                tool(1, "getOrder", "{\"orderId\":\"O1001\"}"));

        TrajectoryEvaluationResult result = evaluator.evaluate(trajectory, expectation);

        assertThat(result.answerPassed()).isTrue();
        assertThat(result.outcomePassed()).isFalse();
        assertThat(result.trajectoryPassed()).isTrue();
        assertThat(result.passed()).isFalse();
    }

    @Test
    void correctToolWithWrongOrderIdFailsParameterCheck() {
        TrajectoryExpectation expectation = TrajectoryEvalCaseCatalog
                .byId("DEV_ORDER_QUERY_O1001").expectation();
        AgentTrajectory trajectory = trajectory(
                "订单 O1001 金额 399 元",
                true,
                tool(1, "getOrder", "{\"orderId\":\"O1002\"}"));

        TrajectoryEvaluationResult result = evaluator.evaluate(trajectory, expectation);

        assertThat(result.answerPassed()).isTrue();
        assertThat(result.trajectoryPassed()).isFalse();
        assertThat(result.violations())
                .contains("动作 getOrder 参数缺少：O1001");
    }

    @Test
    void updateBeforeApprovalFailsRequiredOrder() {
        TrajectoryExpectation expectation = TrajectoryEvalCaseCatalog
                .byId("HOLDOUT_ORDER_UPDATE_APPROVAL").expectation();
        AgentTrajectory trajectory = trajectory(
                "订单 O1003 已更新为 SHIPPED",
                true,
                tool(1, "updateOrderStatus", "{\"orderId\":\"O1003\",\"status\":\"SHIPPED\"}"),
                step(2, TrajectoryStepType.APPROVAL, "approvalGranted", Map.of()));

        TrajectoryEvaluationResult result = evaluator.evaluate(trajectory, expectation);

        assertThat(result.trajectoryPassed()).isFalse();
        assertThat(result.violations())
                .anyMatch(violation -> violation.contains("动作顺序"));
    }

    @Test
    void validTrajectoryPassesAllThreeLayers() {
        TrajectoryExpectation expectation = TrajectoryEvalCaseCatalog
                .byId("HOLDOUT_ORDER_UPDATE_APPROVAL").expectation();
        AgentTrajectory trajectory = trajectory(
                "订单 O1003 已更新为 SHIPPED",
                true,
                step(1, TrajectoryStepType.APPROVAL, "approvalGranted", Map.of()),
                tool(2, "updateOrderStatus",
                        "{\"orderId\":\"O1003\",\"status\":\"SHIPPED\"}"));

        TrajectoryEvaluationResult result = evaluator.evaluate(trajectory, expectation);

        assertThat(result.answerPassed()).isTrue();
        assertThat(result.outcomePassed()).isTrue();
        assertThat(result.trajectoryPassed()).isTrue();
        assertThat(result.passed()).isTrue();
    }

    @Test
    void developmentAndHoldoutInputsAreSeparated() {
        assertThat(TrajectoryEvalCaseCatalog.developmentCases()).isNotEmpty();
        assertThat(TrajectoryEvalCaseCatalog.holdoutCases()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(TrajectoryEvalCaseCatalog.developmentCases())
                .extracting(TrajectoryEvalCase::input)
                .doesNotContainAnyElementsOf(
                        TrajectoryEvalCaseCatalog.holdoutCases().stream()
                                .map(TrajectoryEvalCase::input)
                                .toList());
    }

    private AgentTrajectory trajectory(String answer,
                                       boolean taskCompleted,
                                       TrajectoryStep... steps) {
        return new AgentTrajectory(
                "run-test", "case-test", answer, taskCompleted, 100, List.of(steps));
    }

    private TrajectoryStep tool(int sequence, String action, String arguments) {
        return step(sequence, TrajectoryStepType.TOOL, action, Map.of("arguments", arguments));
    }

    private TrajectoryStep step(int sequence,
                                TrajectoryStepType type,
                                String action,
                                Map<String, String> parameters) {
        return new TrajectoryStep(
                sequence, "TestAgent", type, action, parameters,
                "OK", 10, 0, 0, 0.0);
    }
}
