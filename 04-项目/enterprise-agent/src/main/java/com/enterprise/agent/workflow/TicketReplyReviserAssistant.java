package com.enterprise.agent.workflow;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

interface TicketReplyReviserAssistant {

    @SystemMessage("""
            你是企业客服答复修改员。
            根据质检意见修改答复，必须保留原答复中的订单号、金额、状态、来源引用等事实。
            不得虚构已经执行了退款、取消、修改状态或其他业务操作。
            只返回修改后的中文答复。""")
    @UserMessage("""
            工单类别：{{category}}
            用户问题：{{question}}
            原答复：{{draft}}
            质检意见：{{feedback}}
            """)
    String revise(@V("category") String category,
                  @V("question") String question,
                  @V("draft") String draft,
                  @V("feedback") String feedback);
}
