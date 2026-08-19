package com.enterprise.agent.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 文档入库（Week 3 Day 2）：分块 → Embedding → 写入向量库。
 */
@Service
public class DocumentIngestionService {

    private static final int CHUNK_SIZE = 300;
    private static final int CHUNK_OVERLAP = 30;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentSplitter splitter = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP);

    public DocumentIngestionService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public IngestionResult ingest(String documentId, String content, Map<String, Object> metadata) {
        Metadata md = new Metadata();
        md.put("documentId", documentId);
        if (metadata != null) {
            metadata.forEach((key, value) -> md.put(key, value == null ? "" : value.toString()));
        }

        // 1. 分块：递归按字符切，块大小 300，重叠 30（保证上下文连续性）
        List<TextSegment> segments = splitter.split(Document.from(content, md));

        // 2. Embedding：整批向量化
        Response<List<Embedding>> response = embeddingModel.embedAll(segments);
        List<Embedding> embeddings = response.content();

        // 3. 写入向量库：每个片段一个向量，返回自动生成的 id
        List<String> segmentIds = embeddingStore.addAll(embeddings, segments);

        return new IngestionResult(documentId, segments.size(), segmentIds);
    }
}