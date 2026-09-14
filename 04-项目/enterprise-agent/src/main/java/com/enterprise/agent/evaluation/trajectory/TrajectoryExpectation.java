package com.enterprise.agent.evaluation.trajectory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对一条正确轨迹的机器可检查契约。
 */
public record TrajectoryExpectation(
        List<String> expectedAnswerFragments,
        Set<String> requiredActions,
        Set<String> forbiddenActions,
        List<String> orderedActions,
        Map<String, Set<String>> requiredArgumentFragments,
        boolean requireTaskCompleted
) {

    public TrajectoryExpectation {
        expectedAnswerFragments = List.copyOf(expectedAnswerFragments);
        requiredActions = Set.copyOf(requiredActions);
        forbiddenActions = Set.copyOf(forbiddenActions);
        orderedActions = List.copyOf(orderedActions);
        Map<String, Set<String>> copied = new LinkedHashMap<>();
        requiredArgumentFragments.forEach((action, fragments) ->
                copied.put(action, Set.copyOf(fragments)));
        requiredArgumentFragments = Map.copyOf(copied);
    }
}
