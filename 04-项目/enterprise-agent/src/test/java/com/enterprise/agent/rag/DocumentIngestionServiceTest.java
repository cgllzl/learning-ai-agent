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
    private InMemoryCorpus corpus;
    private DocumentIngestionService service;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        embeddingStore = new InMemoryEmbeddingStore<>();
        corpus = new InMemoryCorpus();
        service = new DocumentIngestionService(embeddingModel, embeddingStore, corpus);

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
        String longText = "Java 是面向对象的编程语言。".repeat(60);
        IngestionResult result = service.ingest("DOC1", longText, null);
        assertThat(result.documentId()).isEqualTo("DOC1");
        assertThat(result.segmentCount()).isGreaterThan(1);
        assertThat(result.segmentIds()).hasSize(result.segmentCount());
    }

    @Test
    void storesDocumentIdInMetadata() {
        service.ingest("DOC1", "Java 21 引入了虚拟线程。", null);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TextSegment>> captor = ArgumentCaptor.forClass(List.class);
        verify(embeddingModel).embedAll(captor.capture());
        assertThat(captor.getValue().get(0).metadata().getString("documentId")).isEqualTo("DOC1");
    }

    @Test
    void alsoWritesKeywordCorpus() {
        service.ingest("DOC2", "入职满一年享有 5 天年假。", null);
        assertThat(corpus.getAll()).isNotEmpty();
    }
}