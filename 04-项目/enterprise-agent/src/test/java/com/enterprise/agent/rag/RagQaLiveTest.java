package com.enterprise.agent.rag;

import com.enterprise.agent.chat.DeepSeekProperties;
import com.enterprise.agent.chat.ResilientCaller;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG 问答端到端联调：真实本地 Embedding 入库 + Hybrid Search + 真实 DeepSeek 生成。
 * 需同时设置 DEEPSEEK_API_KEY 与 RUN_ONNX_TESTS：
 *   .\scripts\test-rag-qa-live.ps1
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "RUN_ONNX_TESTS", matches = ".+")
class RagQaLiveTest {

    @Test
    void answersQuestionFromIngestedDocument() {
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        InMemoryCorpus corpus = new InMemoryCorpus();

        // 1. 入库一篇企业制度文档
        DocumentIngestionService ingestion = new DocumentIngestionService(embeddingModel, store, corpus);
        String content = "我们公司的年假制度：入职满一年享有 5 天年假，满三年享有 10 天年假，"
                + "年假需提前三天向直属主管申请。";
        IngestionResult ingested = ingestion.ingest("HR-001", content, null);
        System.out.println("[入库] 分块数 = " + ingested.segmentCount());

        // 2. 混合检索（向量 + 关键词 + RRF）
        DocumentRetrievalService retrieval = new DocumentRetrievalService(embeddingModel, store);
        HybridSearchService hybrid = new HybridSearchService(retrieval, corpus);

        // 3. 真实 DeepSeek 生成（走容错链路）
        String key = System.getenv("DEEPSEEK_API_KEY");
        DeepSeekProperties props = new DeepSeekProperties(
                key, "https://api.deepseek.com", "deepseek-chat",
                Duration.ofSeconds(60), 2, "deepseek-chat");
        OpenAiChatModel primary = OpenAiChatModel.builder()
                .baseUrl(props.baseUrl()).apiKey(props.apiKey())
                .modelName(props.model()).timeout(props.timeout()).build();
        OpenAiChatModel fallback = OpenAiChatModel.builder()
                .baseUrl(props.baseUrl()).apiKey(props.apiKey())
                .modelName(props.fallbackModel()).timeout(props.timeout()).build();
        RagQaService qa = new RagQaService(hybrid, new ResilientCaller(props, primary, fallback));

        RagChatResponse response = qa.ask("公司年假有几天？", null, 3);

        System.out.println("[RAG 回答] " + response.answer());
        assertThat(response.answer()).isNotBlank();
        assertThat(response.sources()).isNotEmpty();
        assertThat(response.answer()).contains("5");
    }
}