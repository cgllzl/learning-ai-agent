package com.enterprise.agent.agent;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class OrderAgentService {

    private final OrderAssistant assistant;

    public OrderAgentService(@Qualifier("openAiChatModel") OpenAiChatModel chatModel,
                             OrderTools orderTools,
                             AgentProperties agentProperties) {
        this.assistant = AiServices.builder(OrderAssistant.class)
                .chatModel(chatModel)
                .tools(orderTools)
                // 最大连续工具调用次数：防止 Agent 陷入工具调用死循环
                .maxSequentialToolsInvocations(agentProperties.maxSequentialToolsInvocations())
                .build();
    }

    public String chat(String message) {
        return assistant.chat(message);
    }
}