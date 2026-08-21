package com.enterprise.agent.rag;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 运行一组评估用例（Week 3 Day 6）。
 * 每个用例：提问 → RAG 问答 → 计算召回（recall@K）与引用准确率。
 */
@Service
public class RagEvaluationService {

    private static final int DEFAULT_K = 3;
    private static final int DEFAULT_MAX_RESULTS = 5;

    private final RagQaService ragQaService;

    public RagEvaluationService(RagQaService ragQaService) {
        this.ragQaService = ragQaService;
    }

    public RagEvalMetrics evaluate(List<RagEvalCase> cases) {
        int recallHits = 0;
        int citationHits = 0;
        for (RagEvalCase evalCase : cases) {
            RagChatResponse response = ragQaService.ask(evalCase.question(), null, DEFAULT_MAX_RESULTS);
            if (RagEvaluator.recallAtK(response.sources(), evalCase.expectedDocumentId(), DEFAULT_K)) {
                recallHits++;
            }
            if (RagEvaluator.citationCorrect(response.answer(), response.sources(), evalCase.expectedDocumentId())) {
                citationHits++;
            }
        }
        return RagEvaluator.metrics(cases.size(), recallHits, citationHits);
    }
}