package com.enterprise.agent.evaluation;

import com.enterprise.agent.rag.RagEvaluator;
import com.enterprise.agent.rag.RetrievedChunk;

import java.util.List;

/**
 * 自动化评估器（Week 6 Day 2）：
 * - 正确性：检查回答是否包含全部预期关键词/事实；
 * - 引用准确性：复用 RAG 的 citationCorrect，验证 [n] 引用是否指向正确文档。
 */
public class AgentEvaluationService {

    public AgentEvalResult evaluateCorrectness(AgentEvalCase evalCase, String answer) {
        List<String> missing = evalCase.expectedChecks().stream()
                .filter(check -> !answer.contains(check))
                .toList();
        boolean passed = missing.isEmpty();
        String detail = passed
                ? "通过：全部预期检查点命中"
                : "未通过：缺少检查点 " + missing;
        return new AgentEvalResult(evalCase.id(), passed, detail);
    }

    public AgentEvalResult evaluateCitationAccuracy(
            AgentEvalCase evalCase,
            String answer,
            List<RetrievedChunk> sources,
            String expectedDocumentId) {
        boolean passed = RagEvaluator.citationCorrect(answer, sources, expectedDocumentId);
        String detail = passed
                ? "通过：所有引用均指向期望文档"
                : "未通过：引用缺失或指向错误文档";
        return new AgentEvalResult(evalCase.id(), passed, detail);
    }
}
