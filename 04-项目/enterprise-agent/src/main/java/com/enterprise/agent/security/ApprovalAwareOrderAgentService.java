package com.enterprise.agent.security;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

/**
 * 带审计日志 + 人工审批的订单 Agent（Week 5 Day 6）。
 * 通过 PermissionAwareToolProvider 只暴露当前用户有权使用的工具；
 * updateOrderStatus 这类高危工具在 ApprovalAwareOrderTools 里进一步要求人工审批。
 */
public class ApprovalAwareOrderAgentService {

    private final ApprovalAwareOrderAssistant assistant;

    public ApprovalAwareOrderAgentService(OpenAiChatModel chatModel,
                                          ApprovalAwareOrderTools orderTools) {
        this.assistant = AiServices.builder(ApprovalAwareOrderAssistant.class)
                .chatModel(chatModel)
                .toolProvider(new PermissionAwareToolProvider(orderTools))
                .maxSequentialToolsInvocations(3)
                .build();
    }

    public String chat(SecuritySubject subject, String message) {
        return TenantContext.run(subject, () -> assistant.chat(message));
    }
}
