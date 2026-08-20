package com.enterprise.agent.rag;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HybridSearchServiceTest {

    private DocumentRetrievalService retrievalService;
    private InMemoryCorpus corpus;
    private HybridSearchService hybridSearchService;

    @BeforeEach
    void setUp() {
        retrievalService = mock(DocumentRetrievalService.class);
        corpus = new InMemoryCorpus();

        Metadata md1 = new Metadata();
        md1.put("documentId", "DOC1");
        corpus.addAll(List.of(TextSegment.from("虚拟线程可以提升并发吞吐量", md1)));

        hybridSearchService = new HybridSearchService(retrievalService, corpus);
    }

    @Test
    void mergesVectorAndKeywordHits() {
        when(retrievalService.retrieve(anyString(), isNull(), anyInt(), any())).thenReturn(List.of(
                new RetrievedChunk("虚拟线程可以提升并发吞吐量", 0.9, "DOC1")));

        List<RetrievedChunk> result = hybridSearchService.search("什么是虚拟线程", null, 5, 0.0);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).text()).contains("虚拟线程");
        assertThat(result.get(0).documentId()).isEqualTo("DOC1");
    }

    @Test
    void filtersByDocumentId() {
        Metadata md2 = new Metadata();
        md2.put("documentId", "DOC2");
        corpus.addAll(List.of(TextSegment.from("Redis 缓存热点数据", md2)));
        when(retrievalService.retrieve(anyString(), any(), anyInt(), any())).thenReturn(List.of());

        List<RetrievedChunk> result = hybridSearchService.search("虚拟线程", "DOC2", 5, 0.0);

        assertThat(result).allMatch(chunk -> chunk.documentId().equals("DOC2"));
    }
}