package com.enterprise.agent.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 相似度检索 + 元数据过滤（Week 3 Day 3）。
 */
@Service
public class DocumentRetrievalService {

    private static final int DEFAULT_MAX_RESULTS = 5;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public DocumentRetrievalService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public List<RetrievedChunk> retrieve(String query, String documentId, Integer maxResults, Double minScore) {
        // 1. 把查询文本向量化
        Response<Embedding> queryResponse = embeddingModel.embed(query);
        Embedding queryEmbedding = queryResponse.content();

        // 2. 可选：按 documentId 做元数据过滤
        Filter filter = null;
        if (documentId != null && !documentId.isBlank()) {
            filter = MetadataFilterBuilder.metadataKey("documentId").isEqualTo(documentId);
        }

        // 3. 相似度检索，取 TopK
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults == null ? DEFAULT_MAX_RESULTS : maxResults)
                .minScore(minScore == null ? 0.0 : minScore)
                .filter(filter)
                .build();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        // 4. 转成对外的结果（文本 + 相似度 + 来源文档）
        return result.matches().stream()
                .map(this::toRetrievedChunk)
                .toList();
    }

    private RetrievedChunk toRetrievedChunk(EmbeddingMatch<TextSegment> match) {
        String documentId = match.embedded().metadata().getString("documentId");
        return new RetrievedChunk(match.embedded().text(), match.score(), documentId);
    }
}