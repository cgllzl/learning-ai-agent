package com.enterprise.agent.rag;

import com.enterprise.agent.chat.ResilientCaller;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 问答链路（Week 3 Day 4；Day 5 改用 Hybrid Search）。
 */
@Service
public class RagQaService {

    private static final int DEFAULT_MAX_RESULTS = 5;

    private static final String SYSTEM_PROMPT = """
            你是一个企业知识助手。请只根据【参考资料】回答用户的问题，不要编造。
            引用资料时用 [序号] 标注来源，例如 [1][2]。
            如果参考资料里没有答案，请直接说明"资料中没有相关内容"。
            回答用中文，简洁准确。""";

    private final HybridSearchService hybridSearchService;
    private final ResilientCaller resilientCaller;

    public RagQaService(HybridSearchService hybridSearchService, ResilientCaller resilientCaller) {
        this.hybridSearchService = hybridSearchService;
        this.resilientCaller = resilientCaller;
    }

    public RagChatResponse ask(String question, String documentId, Integer maxResults) {
        // 1. 混合检索（向量 + 关键词 + RRF 重排）
        List<RetrievedChunk> chunks = hybridSearchService.search(
                question, documentId, maxResults, 0.0);

        // 2. 拼 Prompt：编号后的参考资料 + 问题
        String context = buildContext(chunks);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM_PROMPT));
        messages.add(UserMessage.from("【参考资料】\n" + context + "\n\n【问题】\n" + question));

        // 3. 生成（走容错：重试/备用模型）
        ChatResponse response = resilientCaller.callWithFallback(model -> model.chat(messages));
        String answer = response.aiMessage().text();

        // 4. 返回回答 + 引用来源
        return new RagChatResponse(answer, chunks);
    }

    private String buildContext(List<RetrievedChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            sb.append("[").append(i + 1).append("] ")
                    .append("(来源: ").append(chunk.documentId()).append(") ")
                    .append(chunk.text()).append("\n");
        }
        return sb.toString();
    }
}