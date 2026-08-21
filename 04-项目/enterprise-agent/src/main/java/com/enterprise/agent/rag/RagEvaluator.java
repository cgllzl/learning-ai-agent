package com.enterprise.agent.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 评估指标计算（Week 3 Day 6，Day 6 补充：严格引用 + 引用精确率）。
 * - recall@K：期望文档是否出现在前 K 条检索结果里（衡量召回）
 * - citationAccuracy：每道题所有 [n] 引用都正确才算过（严格版）
 * - citationPrecision：全部用例的正确引用数 / 总引用数
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

    /** 严格版：至少有一处引用，且每一处引用都必须指向期望文档、下标合法。 */
    public static boolean citationCorrect(String answer, List<RetrievedChunk> sources, String expectedDocumentId) {
        List<Integer> indices = citationIndices(answer);
        if (indices.isEmpty()) {
            return false;
        }
        return indices.stream().allMatch(index -> isCorrect(index, sources, expectedDocumentId));
    }

    /** 引用精确率：正确引用数 / 总引用数（0~1）。 */
    public static double citationPrecision(String answer, List<RetrievedChunk> sources, String expectedDocumentId) {
        List<Integer> indices = citationIndices(answer);
        if (indices.isEmpty()) {
            return 0.0;
        }
        long correct = indices.stream()
                .filter(index -> isCorrect(index, sources, expectedDocumentId))
                .count();
        return (double) correct / indices.size();
    }

    public static int citationCount(String answer) {
        return citationIndices(answer).size();
    }

    public static int correctCitationCount(String answer, List<RetrievedChunk> sources, String expectedDocumentId) {
        return (int) citationIndices(answer).stream()
                .filter(index -> isCorrect(index, sources, expectedDocumentId))
                .count();
    }

    public static RagEvalMetrics metrics(int total, int recallHits, int citationHits, double citationPrecision) {
        double recallRate = total == 0 ? 0.0 : (double) recallHits / total;
        double citationAccuracy = total == 0 ? 0.0 : (double) citationHits / total;
        return new RagEvalMetrics(total, recallHits, citationHits, recallRate, citationAccuracy, citationPrecision);
    }

    private static boolean isCorrect(int index, List<RetrievedChunk> sources, String expectedDocumentId) {
        return index >= 0 && index < sources.size()
                && expectedDocumentId.equals(sources.get(index).documentId());
    }

    private static List<Integer> citationIndices(String answer) {
        List<Integer> indices = new ArrayList<>();
        Matcher matcher = CITATION_PATTERN.matcher(answer);
        while (matcher.find()) {
            indices.add(Integer.parseInt(matcher.group(1)) - 1); // 1 起转 0 起
        }
        return indices;
    }
}