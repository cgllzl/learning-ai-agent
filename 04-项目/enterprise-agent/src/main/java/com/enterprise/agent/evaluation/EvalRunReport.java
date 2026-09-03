package com.enterprise.agent.evaluation;

import java.util.List;

/**
 * 一组评估用例的运行报告。
 */
public record EvalRunReport(
        int total,
        int passed,
        int failed,
        List<EvalFailure> failures) {

    public boolean success() {
        return failed == 0;
    }
}
