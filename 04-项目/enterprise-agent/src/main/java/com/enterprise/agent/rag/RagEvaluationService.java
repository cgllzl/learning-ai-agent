package com.enterprise.agent.rag;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 运行一组评估用例（Week 3 Day 6）。
 * 每个用例：提问 → RAG 问答 → 计算召回、严格引用正确率、引用精确率。
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
        int totalCitations = 0;
        int totalCorrectCitations = 0;

        for (RagEvalCase evalCase : cases) {
            RagChatResponse response = ragQaService.ask(evalCase.question(), null, DEFAULT_MAX_RESULTS);

            // 召回数和引用数和用户输入的case数有关
            if (RagEvaluator.recallAtK(response.sources(), evalCase.expectedDocumentId(), DEFAULT_K)) {
                recallHits++;
            }
            if (RagEvaluator.citationCorrect(response.answer(), response.sources(), evalCase.expectedDocumentId())) {
                citationHits++;
            }

            // 总召回数和总正确引用数和大模型返回的结果有关
            totalCitations += RagEvaluator.citationCount(response.answer());
            totalCorrectCitations += RagEvaluator.correctCitationCount(
                    response.answer(), response.sources(), evalCase.expectedDocumentId());
        }

        double citationPrecision = totalCitations == 0
                ? 0.0
                : (double) totalCorrectCitations / totalCitations;
        return RagEvaluator.metrics(cases.size(), recallHits, citationHits, citationPrecision);
    }
}