package com.enterprise.agent.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkbenchRoutingServiceTest {

    @Test
    @DisplayName("目的：订单与知识问题只给对应页面建议，不触发业务执行")
    void orderAndKnowledgeQuestionsProduceReadOnlyNavigation() {
        WorkbenchRouteResult order = new WorkbenchRoutingService(message -> "ORDER")
                .route("查询订单 O1001");
        WorkbenchRouteResult knowledge = new WorkbenchRoutingService(message -> "KNOWLEDGE")
                .route("公司的报销制度是什么");

        assertThat(order.category()).isEqualTo(TicketCategory.ORDER);
        assertThat(order.suggestedView()).isEqualTo(WorkbenchView.ORDERS);
        assertThat(knowledge.category()).isEqualTo(TicketCategory.KNOWLEDGE);
        assertThat(knowledge.suggestedView()).isEqualTo(WorkbenchView.KNOWLEDGE);
        assertThat(order.executionStatus()).isEqualTo("NOT_EXECUTED");
        assertThat(knowledge.executionStatus()).isEqualTo("NOT_EXECUTED");
    }

    @Test
    @DisplayName("目的：模型输出含糊或输入为空时关闭式转人工，且不继续分类空输入")
    void ambiguousOrBlankInputRoutesToHuman() {
        WorkbenchRouteResult ambiguous = new WorkbenchRoutingService(
                message -> "ORDER or KNOWLEDGE").route("帮我处理一下");
        WorkbenchRouteResult blank = new WorkbenchRoutingService(
                message -> { throw new AssertionError("空输入不应调用模型"); }).route(" ");

        assertThat(ambiguous.suggestedView()).isEqualTo(WorkbenchView.HUMAN_QUEUE);
        assertThat(ambiguous.requiresHumanReview()).isTrue();
        assertThat(blank.suggestedView()).isEqualTo(WorkbenchView.HUMAN_QUEUE);
        assertThat(blank.executionStatus()).isEqualTo("NOT_EXECUTED");
    }
}
