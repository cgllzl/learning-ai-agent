package com.enterprise.agent.rag;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentRetrievalServiceTest {

    private EmbeddingModel embeddingModel;
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private DocumentRetrievalService service;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        embeddingStore = new InMemoryEmbeddingStore<>();
        service = new DocumentRetrievalService(embeddingModel, embeddingStore);

        // 入库两篇文档，向量不同
        Metadata md1 = new Metadata();
        md1.put("documentId", "DOC1");
        Metadata md2 = new Metadata();
        md2.put("documentId", "DOC2");
        embeddingStore.add(new Embedding(new float[]{0.9f, 0.1f, 0.1f}), TextSegment.from("虚拟线程提升并发吞吐量", md1));
        embeddingStore.add(new Embedding(new float[]{0.1f, 0.9f, 0.1f}), TextSegment.from("Redis 缓存热点数据", md2));
    }

    @Test
    void retrievesMostSimilarChunk() {
        when(embeddingModel.embed("什么是虚拟线程")).thenReturn(
                Response.from(new Embedding(new float[]{0.8f, 0.1f, 0.1f})));

        List<RetrievedChunk> chunks = service.retrieve("什么是虚拟线程", null, 2, 0.0);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).text()).contains("虚拟线程");
        assertThat(chunks.get(0).documentId()).isEqualTo("DOC1");
    }

    @Test
    void filtersByDocumentId() {
        when(embeddingModel.embed("查询")).thenReturn(
                Response.from(new Embedding(new float[]{0.1f, 0.1f, 0.9f})));

        List<RetrievedChunk> chunks = service.retrieve("查询", "DOC2", 5, 0.0);

        assertThat(chunks).allMatch(c -> c.documentId().equals("DOC2"));
    }
}