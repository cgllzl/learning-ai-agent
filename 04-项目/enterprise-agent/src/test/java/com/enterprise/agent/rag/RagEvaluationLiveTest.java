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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG 评估端到端联调：真实 Embedding 入库多篇文档，计算 recall@3 与引用准确率。
 * 需同时设置 DEEPSEEK_API_KEY 与 RUN_ONNX_TESTS。
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "RUN_ONNX_TESTS", matches = ".+")
class RagEvaluationLiveTest {

    @Test
    void evaluatesRetrievalAndCitationQuality() {
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        InMemoryCorpus corpus = new InMemoryCorpus();
        DocumentIngestionService ingestion = new DocumentIngestionService(embeddingModel, store, corpus);

        ingestion.ingest("HR-001", "入职满一年享有 5 天年假，满三年享有 10 天年假。", null);
        ingestion.ingest("FIN-001", "单笔报销超过 500 元需要部门经理审批，发票需当月提交。", null);
        ingestion.ingest("IT-001", "新员工入职标配联想 ThinkPad X1 笔记本电脑。", null);

        DocumentRetrievalService retrieval = new DocumentRetrievalService(embeddingModel, store);
        HybridSearchService hybrid = new HybridSearchService(retrieval, corpus);

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
        RagEvaluationService evaluationService = new RagEvaluationService(qa);

        RagEvalMetrics metrics = evaluationService.evaluate(List.of(
                new RagEvalCase("年假有几天？", "HR-001"),
                new RagEvalCase("报销超过多少钱需要审批？", "FIN-001"),
                new RagEvalCase("新员工电脑是什么型号？", "IT-001")));

        System.out.println("[评估结果] " + metrics);
        assertThat(metrics.recallRate()).isGreaterThanOrEqualTo(2.0 / 3.0);
    }
}