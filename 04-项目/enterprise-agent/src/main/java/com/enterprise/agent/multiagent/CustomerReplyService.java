package com.enterprise.agent.multiagent;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 客服回复 Agent 的服务封装。
 * 接收第一个 Agent 产出的状态（订单事实），交给大模型合并润色。
 */
@Service
public class CustomerReplyService {

    private final CustomerReplyAssistant assistant;

    public CustomerReplyService(@Qualifier("openAiChatModel") OpenAiChatModel chatModel) {
        this.assistant = AiServices.builder(CustomerReplyAssistant.class)
                .chatModel(chatModel)
                .build();
    }

    public String compose(String question, String orderFacts) {
        return assistant.compose(question, orderFacts);
    }
}
