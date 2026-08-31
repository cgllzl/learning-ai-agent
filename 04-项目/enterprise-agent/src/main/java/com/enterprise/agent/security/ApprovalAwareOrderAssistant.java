package com.enterprise.agent.security;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ApprovalAwareOrderAssistant {

    @SystemMessage("你是企业订单助手。你可以调用当前可用的工具查询或修改订单。回答用中文，简洁准确。")
    String chat(@UserMessage String message);
}
