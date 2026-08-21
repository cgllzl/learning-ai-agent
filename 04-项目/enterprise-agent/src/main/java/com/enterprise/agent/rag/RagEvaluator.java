package com.enterprise.agent.rag;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 评估指标计算（Week 3 Day 6）。
 * - recall@K：期望文档是否出现在前 K 条检索结果里（衡量召回）
 * - citationAccuracy：回答的 [n] 引用是否指向正确的来源（衡量引用）
 */
public final class RagEvaluator {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(\\d+)\\]");

    private RagEvaluator() {
    }

    public static boolean recallAtK(List<RetrievedChunk> chunks, String expectedDocumentId, int k) {
        return chunks.stream()
                .limit(k)
                .anyMatch(chunk -> expectedDocumentId.equals(chunk.documentId()));
    }

    public static boolean citationCorrect(String answer, List<RetrievedChunk> sources, String expectedDocumentId) {
        Matcher matcher = CITATION_PATTERN.matcher(answer);
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1)) - 1; // 引用是 1 起，转 0 起下标
            if (index >= 0 && index < sources.size()
                    && expectedDocumentId.equals(sources.get(index).documentId())) {
                return true;
            }
        }
        return false;
    }

    public static RagEvalMetrics metrics(int total, int recallHits, int citationHits) {
        double recallRate = total == 0 ? 0.0 : (double) recallHits / total;
        double citationAccuracy = total == 0 ? 0.0 : (double) citationHits / total;
        return new RagEvalMetrics(total, recallHits, citationHits, recallRate, citationAccuracy);
    }
}