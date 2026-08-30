package com.enterprise.agent.security;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

/**
 * 接入 RBAC 与租户隔离的订单 Agent。
 *
 * 流程：
 * 1. RBAC：先检查当前用户是否有 CUSTOMER_SERVICE 或 SUPERVISOR 角色；
 * 2. 租户隔离：把 SecuritySubject 放进 TenantContext，让工具只能读当前租户的数据；
 * 3. 调用大模型回答问题。
 */
public class SecureOrderAgentService {

    private final RbacService rbacService;
    private final TenantOrderAssistant assistant;

    public SecureOrderAgentService(OpenAiChatModel chatModel, RbacService rbacService) {
        this.rbacService = rbacService;
        this.assistant = AiServices.builder(TenantOrderAssistant.class)
                .chatModel(chatModel)
                .tools(new TenantOrderTools(new TenantScopedOrderData()))
                .build();
    }

    public String chat(SecuritySubject subject, String message) {
        rbacService.checkAnyRole(subject, "CUSTOMER_SERVICE", "SUPERVISOR");
        return TenantContext.run(subject, () -> assistant.chat(message));
    }
}
