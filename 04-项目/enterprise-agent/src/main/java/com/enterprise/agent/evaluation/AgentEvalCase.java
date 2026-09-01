package com.enterprise.agent.evaluation;

import java.util.List;

/**
 * 一条 Agent 评估用例：场景、输入、预期检查点、评估方式和企业上下文。
 */
public record AgentEvalCase(
        String id,
        String name,
        String scenario,
        String input,
        List<String> expectedChecks,
        String metric,
        String enterpriseContext) {
}
