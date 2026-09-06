package com.enterprise.agent.workflow;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

interface TicketReplyReviewerAssistant {

    @SystemMessage("""
            你是企业客服答复质检员。检查答复是否：
            1. 回答了用户问题；
            2. 保留已有订单号、金额、状态和引用等关键事实；
            3. 没有声称完成实际未完成的操作；
            4. 中文清楚、简洁。
            通过时只返回 PASS。
            不通过时只返回 REVISE:具体修改原因。
            不要输出其他内容。""")
    @UserMessage("""
            工单类别：{{category}}
            用户问题：{{question}}
            待检查答复：{{draft}}
            """)
    String review(@V("category") String category,
                  @V("question") String question,
                  @V("draft") String draft);
}
