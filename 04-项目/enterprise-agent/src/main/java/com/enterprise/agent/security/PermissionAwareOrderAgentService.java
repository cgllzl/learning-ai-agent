package com.enterprise.agent.security;

import com.enterprise.agent.agent.OrderTools;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

/**
 * Tool 级权限校验的订单 Agent（Week 5 Day 3）。
 * 不再把整个 OrderTools 一次性塞给模型，而是通过 PermissionAwareToolProvider
 * 根据当前用户角色动态决定「模型能看见哪些工具」。
 */
public class PermissionAwareOrderAgentService {

    private final PermissionAwareOrderAssistant assistant;

    public PermissionAwareOrderAgentService(OpenAiChatModel chatModel, OrderTools orderTools) {
        this.assistant = AiServices.builder(PermissionAwareOrderAssistant.class)
                .chatModel(chatModel)
                .toolProvider(new PermissionAwareToolProvider(orderTools))
                .maxSequentialToolsInvocations(3)
                .build();
    }

    public String chat(SecuritySubject subject, String message) {
        return TenantContext.run(subject, () -> assistant.chat(message));
    }
}
