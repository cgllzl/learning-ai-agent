package com.enterprise.agent.supervisor;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SupervisorAgentService {

    private final SupervisorAssistant assistant;

    public SupervisorAgentService(@Qualifier("openAiChatModel") OpenAiChatModel chatModel,
                                  SupervisorTools supervisorTools) {
        this.assistant = AiServices.builder(SupervisorAssistant.class)
                .chatModel(chatModel)
                .tools(supervisorTools)
                .maxSequentialToolsInvocations(3)
                .build();
    }

    public String chat(String message) {
        return assistant.chat(message);
    }
}