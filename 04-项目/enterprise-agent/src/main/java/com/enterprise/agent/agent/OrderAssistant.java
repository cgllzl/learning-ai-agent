package com.enterprise.agent.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface OrderAssistant {

    @SystemMessage("你是企业订单助手。你可以调用工具查询订单信息。回答用中文，简洁准确。")
    String chat(@UserMessage String message);
}