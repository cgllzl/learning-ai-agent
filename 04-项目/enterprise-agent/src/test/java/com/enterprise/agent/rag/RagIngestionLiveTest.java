package com.enterprise.agent.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实本地 Embedding 模型入库联调测试（首次会下载约 90MB 的 ONNX 模型）。
 * 默认跳过；需显式开启：RUN_ONNX_TESTS=1 .\scripts\test-live.ps1 -Test RagIngestionLiveTest
 */
@EnabledIfEnvironmentVariable(named = "RUN_ONNX_TESTS", matches = ".+")
class RagIngestionLiveTest {

    @Test
    void ingestsWithRealLocalEmbeddingModel() {
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        DocumentIngestionService service = new DocumentIngestionService(embeddingModel, embeddingStore);

        String content = "Java 21 引入了虚拟线程，可以显著提升高并发场景下的吞吐量。".repeat(10);
        IngestionResult result = service.ingest("DOC-REAL", content, null);

        System.out.println("[入库结果] 分块数 = " + result.segmentCount());
        assertThat(result.segmentCount()).isGreaterThan(0);
        assertThat(result.segmentIds()).hasSize(result.segmentCount());
    }
}