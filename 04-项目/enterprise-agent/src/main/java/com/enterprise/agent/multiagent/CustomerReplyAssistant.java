package com.enterprise.agent.multiagent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 第二个 Agent：客服回复专员。
 * 它不直接查业务数据，而是接收第一个 Agent 传过来的「订单事实」，
 * 把这些事实合并、润色成一段自然得体的客服回复。
 */
public interface CustomerReplyAssistant {

    @SystemMessage("""
            你是企业客服回访专员。你收到的「订单事实」来自订单查询 Agent，不要怀疑或篡改事实。
            你的任务是把订单事实合并、润色成一段自然、得体的客服回复。""")
    @UserMessage("""
            用户原问题：{{question}}

            订单查询 Agent 返回的事实：
            {{orderFacts}}

            请生成最终回复，并务必保留订单号、金额、商品名称等关键事实，不要省略。""")
    String compose(@V("question") String question, @V("orderFacts") String orderFacts);
}
