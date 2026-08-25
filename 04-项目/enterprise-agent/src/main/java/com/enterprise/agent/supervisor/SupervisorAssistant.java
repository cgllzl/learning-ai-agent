package com.enterprise.agent.supervisor;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface SupervisorAssistant {

    @SystemMessage("""
            你是企业 AI 助手的总调度员。
            先判断用户问题属于哪一类：
            - 订单、用户、商品、物流、修改状态 → 调用 handleOrder
            - 公司制度、文档、知识问答 → 调用 handleKnowledge
            然后基于子助手返回的结果，用中文简洁地回答用户。""")
    String chat(@UserMessage String message);
}