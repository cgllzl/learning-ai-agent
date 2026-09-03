package com.enterprise.agent.evaluation;

/**
 * 一条失败用例的复盘信息。
 */
public record EvalFailure(String caseId, String actualAnswer, String reason) {
}
