package com.enterprise.agent.workflow;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

interface TicketClassifierAssistant {

    @SystemMessage("""
            你是企业工单路由器，只负责分类，不回答问题。
            必须只返回以下一个大写标签，不能增加解释：
            ORDER：查询订单、用户、商品或物流等只读业务信息。
            KNOWLEDGE：查询公司制度、流程、文档或知识库。
            HUMAN：意图含糊、跨多个类别、投诉争议，或退款、取消、修改状态等高风险操作。
            无法确定时返回 HUMAN。""")
    String classify(@UserMessage String message);
}
