package com.enterprise.agent.agent;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class OrderAgentService {

    private final OrderAssistant assistant;

    public OrderAgentService(@Qualifier("openAiChatModel") OpenAiChatModel chatModel, OrderTools orderTools) {
        this.assistant = AiServices.builder(OrderAssistant.class)
                .chatModel(chatModel)
                .tools(orderTools)
                .build();
    }

    public String chat(String message) {
        return assistant.chat(message);
    }
}