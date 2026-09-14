package com.enterprise.agent.evaluation.trajectory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 同时检查最终答案、调用方提供的业务完成标记和中间执行路线。
 * 三层分别记分，便于定位到底是“说错了”“没做成”还是“走错了流程”。
 */
public class TrajectoryEvaluator {

    public TrajectoryEvaluationResult evaluate(AgentTrajectory trajectory,
                                               TrajectoryExpectation expectation) {
        // 三类问题分开收集：例如答案正确但误调用写 Tool，仍能明确看到是轨迹层失败。
        List<String> answerViolations = new ArrayList<>();
        List<String> outcomeViolations = new ArrayList<>();
        List<String> trajectoryViolations = new ArrayList<>();
        if (expectation.requireTaskCompleted() && !trajectory.taskCompleted()) {
            outcomeViolations.add("任务没有真实完成");
        }

        for (String fragment : expectation.expectedAnswerFragments()) {
            if (trajectory.answer() == null || !trajectory.answer().contains(fragment)) {
                answerViolations.add("最终答案缺少检查点：" + fragment);
            }
        }

        List<String> actions = trajectory.steps().stream()
                .map(TrajectoryStep::action)
                .toList();
        expectation.requiredActions().stream()
                .filter(required -> !actions.contains(required))
                .forEach(required -> trajectoryViolations.add("缺少必需动作：" + required));
        expectation.forbiddenActions().stream()
                .filter(actions::contains)
                .forEach(forbidden -> trajectoryViolations.add("出现禁止动作：" + forbidden));

        checkOrder(actions, expectation.orderedActions(), trajectoryViolations);
        checkArguments(trajectory.steps(), expectation.requiredArgumentFragments(), trajectoryViolations);

        trajectory.steps().stream()
                .filter(step -> "ERROR".equalsIgnoreCase(step.status())
                        || "BLOCKED".equalsIgnoreCase(step.status()))
                .forEach(step -> trajectoryViolations.add(
                        "轨迹包含失败步骤：" + step.action() + "=" + step.status()));

        List<String> violations = new ArrayList<>();
        violations.addAll(answerViolations);
        violations.addAll(outcomeViolations);
        violations.addAll(trajectoryViolations);
        boolean answerPassed = answerViolations.isEmpty();
        boolean outcomePassed = outcomeViolations.isEmpty();
        boolean trajectoryPassed = trajectoryViolations.isEmpty();
        // 总结果是硬门禁（AND），不是可互相抵消的加权总分。
        return new TrajectoryEvaluationResult(
                answerPassed,
                outcomePassed,
                trajectoryPassed,
                answerPassed && outcomePassed && trajectoryPassed,
                violations);
    }

    private void checkOrder(List<String> actualActions,
                            List<String> expectedOrder,
                            List<String> violations) {
        // 检查期望动作是否按顺序成为实际轨迹的一个“子序列”；中间允许夹杂其他合法动作。
        int previousIndex = -1;
        for (String action : expectedOrder) {
            int index = nextIndexOf(actualActions, action, previousIndex + 1);
            if (index < 0) {
                violations.add("动作顺序不满足要求，缺少或位置错误：" + action);
                return;
            }
            previousIndex = index;
        }
    }

    private int nextIndexOf(List<String> actions, String target, int fromIndex) {
        for (int i = fromIndex; i < actions.size(); i++) {
            if (target.equals(actions.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private void checkArguments(List<TrajectoryStep> steps,
                                Map<String, Set<String>> expectations,
                                List<String> violations) {
        // 学习版在参数摘要中做片段匹配；生产环境应解析结构化参数并按字段类型、范围逐项校验。
        expectations.forEach((action, fragments) -> {
            List<TrajectoryStep> matchingSteps = steps.stream()
                    .filter(step -> action.equals(step.action()))
                    .toList();
            if (matchingSteps.isEmpty()) {
                violations.add("无法检查参数，轨迹中没有动作：" + action);
                return;
            }
            String parameterText = matchingSteps.stream()
                    .flatMap(step -> step.parameterSummary().values().stream())
                    .reduce("", (left, right) -> left + " " + right);
            fragments.stream()
                    .filter(fragment -> !parameterText.contains(fragment))
                    .forEach(fragment -> violations.add(
                            "动作 " + action + " 参数缺少：" + fragment));
        });
    }
}
