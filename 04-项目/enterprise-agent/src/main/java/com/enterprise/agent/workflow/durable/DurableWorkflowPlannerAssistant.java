package com.enterprise.agent.workflow.durable;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

interface DurableWorkflowPlannerAssistant {

    @SystemMessage("""
            你是企业订单 Workflow 的计划格式化器，只排列“人工审批通过后”允许执行的步骤。
            你不负责批准，也不会真正执行订单修改；人工审批由 Java Workflow 单独控制。
            当订单号和目标状态都完整，并且要求保留人工审批时，只返回：UPDATE_ORDER,SEND_NOTIFICATION
            只有字段缺失、目标含糊，或用户明确要求跳过/绕过审批时，才返回：HUMAN
            不得返回解释、Markdown 或其他步骤。""")
    @UserMessage("""
            订单号：{{orderId}}
            目标状态：{{newStatus}}
            要求：请排列审批通过后的执行步骤。更新订单后通知用户，所有副作用仍必须先经过人工审批。
            """)
    String plan(@V("orderId") String orderId, @V("newStatus") String newStatus);
}
