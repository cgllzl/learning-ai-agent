package com.enterprise.agent.security;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface TenantOrderAssistant {

    @SystemMessage("你是企业订单助手。你可以调用 getOrder 工具查询当前租户内的订单信息。回答用中文，简洁准确。")
    String chat(@UserMessage String message);
}
