package com.enterprise.agent.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 评估回归执行器（Week 6 Day 6）。
 * 给定一组用例和「答案提供者」，返回通过/失败报告。
 */
public class EvalRegressionRunner {

    private final AgentEvaluationService evaluator;

    public EvalRegressionRunner(AgentEvaluationService evaluator) {
        this.evaluator = evaluator;
    }

    public EvalRunReport run(List<AgentEvalCase> cases,
                             Function<AgentEvalCase, String> answerProvider) {
        int passed = 0;
        List<EvalFailure> failures = new ArrayList<>();

        for (AgentEvalCase evalCase : cases) {
            String answer = answerProvider.apply(evalCase);
            AgentEvalResult result = evaluator.evaluateCorrectness(evalCase, answer);
            if (result.passed()) {
                passed++;
            } else {
                failures.add(new EvalFailure(evalCase.id(), answer, result.detail()));
            }
        }

        return new EvalRunReport(
                cases.size(),
                passed,
                failures.size(),
                List.copyOf(failures));
    }
}
