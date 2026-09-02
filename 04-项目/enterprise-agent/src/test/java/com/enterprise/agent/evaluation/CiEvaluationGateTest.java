package com.enterprise.agent.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CI 离线评估门禁：不调用大模型，用固定答案验证核心评估逻辑全部通过。
 * CI 环境通常没有 DeepSeek Key，因此这里只跑离线评估。
 */
class CiEvaluationGateTest {

    private final AgentEvaluationService evaluator = new AgentEvaluationService();

    @Test
    void offlineCoreCasesAllPass() {
        List<AgentEvalResult> results = List.of(
                evaluator.evaluateCorrectness(
                        AgentEvalCaseCatalog.byId("ORDER_QUERY"),
                        "订单 O1001 金额 399 元"),
                evaluator.evaluateCorrectness(
                        AgentEvalCaseCatalog.byId("MULTI_AGENT_MERGE"),
                        "您好，您的订单 O1001 金额 399 元，将尽快为您安排。"),
                evaluator.evaluateCorrectness(
                        AgentEvalCaseCatalog.byId("TENANT_ISOLATION"),
                        "租户 t1 的订单金额 399；租户 t2 的订单金额 1299。"));

        assertThat(results).allSatisfy(result -> assertThat(result.passed()).isTrue());
    }
}
