package com.enterprise.agent.evaluation;

/**
 * 一条用例的评估结果。
 */
public record AgentEvalResult(String caseId, boolean passed, String detail) {
}
