package com.enterprise.agent.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentIngestionServiceTest {

    private EmbeddingModel embeddingModel;
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private DocumentIngestionService service;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        embeddingStore = new InMemoryEmbeddingStore<>();
        service = new DocumentIngestionService(embeddingModel, embeddingStore);

        // 用固定向量模拟 Embedding，避免单测下载本地模型
        when(embeddingModel.embedAll(anyList())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(0);
            List<Embedding> embeddings = segments.stream()
                    .map(s -> new Embedding(new float[]{0.1f, 0.2f, 0.3f}))
                    .toList();
            return Response.from(embeddings);
        });
    }

    @Test
    void splitsLongTextIntoMultipleSegments() {
        String longText = "Java 是面向对象的编程语言。".repeat(60); // 超过 300 字，会被分块

        IngestionResult result = service.ingest("DOC1", longText, null);

        assertThat(result.documentId()).isEqualTo("DOC1");
        assertThat(result.segmentCount()).isGreaterThan(1);
        assertThat(result.segmentIds()).hasSize(result.segmentCount());
    }

    @Test
    void storesDocumentIdInMetadata() {
        String shortText = "Java 21 引入了虚拟线程。";

        service.ingest("DOC1", shortText, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TextSegment>> captor = ArgumentCaptor.forClass(List.class);
        verify(embeddingModel).embedAll(captor.capture());
        TextSegment segment = captor.getValue().get(0);

        assertThat(segment.metadata().getString("documentId")).isEqualTo("DOC1");
    }

    @Test
    void storesEachSegmentInEmbeddingStore() {
        String text = "第一段内容，用来测试向量库写入。".repeat(40);

        IngestionResult result = service.ingest("DOC2", text, null);

        assertThat(result.segmentCount()).isGreaterThan(0);
        assertThat(embeddingStore.serializeToJson()).contains("DOC2");
    }
}