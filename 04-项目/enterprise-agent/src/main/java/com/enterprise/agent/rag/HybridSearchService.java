package com.enterprise.agent.rag;

import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hybrid Search + Reranking（Week 3 Day 5）。
 * 向量检索（语义）+ 关键词检索（字面），用 RRF（倒数排名融合）重排合并。
 */
@Service
public class HybridSearchService {

    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final double RRF_K = 60;

    private final DocumentRetrievalService retrievalService;
    private final InMemoryCorpus corpus;

    public HybridSearchService(DocumentRetrievalService retrievalService, InMemoryCorpus corpus) {
        this.retrievalService = retrievalService;
        this.corpus = corpus;
    }

    public List<RetrievedChunk> search(String query, String documentId, Integer maxResults, Double minScore) {
        int topK = maxResults == null ? DEFAULT_MAX_RESULTS : maxResults;

        // 1. 向量检索（语义）：多召回一些候选
        List<RetrievedChunk> vectorHits = retrievalService.retrieve(query, documentId, topK * 2, minScore == null ? 0.0 : minScore);

        // 2. 关键词检索（字面）：用字符 bigram 打分
        List<RetrievedChunk> keywordHits = keywordSearch(query, documentId, topK * 2);

        // 3. RRF 重排合并
        return reciprocalRankFusion(vectorHits, keywordHits, topK);
    }

    private List<RetrievedChunk> keywordSearch(String query, String documentId, int maxResults) {
        Set<String> queryGrams = bigrams(query);
        return corpus.getAll().stream()
                .filter(segment -> documentId == null
                        || documentId.equals(segment.metadata().getString("documentId")))
                .map(segment -> {
                    double score = overlap(queryGrams, bigrams(segment.text()));
                    return new RetrievedChunk(segment.text(), score, segment.metadata().getString("documentId"));
                })
                .filter(chunk -> chunk.score() > 0)
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(maxResults)
                .toList();
    }

    private Set<String> bigrams(String text) {
        Set<String> grams = new HashSet<>();
        String cleaned = text.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");
        for (int i = 0; i + 1 < cleaned.length(); i++) {
            grams.add(cleaned.substring(i, i + 2));
        }
        return grams;
    }

    private double overlap(Set<String> a, Set<String> b) {
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        return intersection.size();
    }

    private List<RetrievedChunk> reciprocalRankFusion(List<RetrievedChunk> listA, List<RetrievedChunk> listB, int maxResults) {
        Map<String, RrfEntry> merged = new LinkedHashMap<>();
        accumulate(merged, listA);
        accumulate(merged, listB);
        return merged.values().stream()
                .sorted(Comparator.comparingDouble(RrfEntry::score).reversed())
                .limit(maxResults)
                .map(entry -> new RetrievedChunk(entry.chunk.text(), entry.score, entry.chunk.documentId()))
                .toList();
    }

    private void accumulate(Map<String, RrfEntry> merged, List<RetrievedChunk> list) {
        for (int i = 0; i < list.size(); i++) {
            RetrievedChunk chunk = list.get(i);
            String key = chunk.documentId() + "||" + chunk.text();
            RrfEntry entry = merged.computeIfAbsent(key, k -> new RrfEntry(chunk));
            entry.score += 1.0 / (RRF_K + i + 1);
        }
    }

    private static final class RrfEntry {
        final RetrievedChunk chunk;
        double score;

        RrfEntry(RetrievedChunk chunk) {
            this.chunk = chunk;
            this.score = 0.0;
        }

        double score() {
            return score;
        }
    }
}