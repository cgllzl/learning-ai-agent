package com.enterprise.agent.workflow;

import com.enterprise.agent.agent.OrderAgentService;
import com.enterprise.agent.rag.RagQaService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoopWorkflowTest {

    @Test
    void revisesDraftUntilReviewPasses() {
        TicketClassifierAssistant classifier = message -> "ORDER";
        TicketReplyReviewerAssistant reviewer = mock(TicketReplyReviewerAssistant.class);
        TicketReplyReviserAssistant reviser = mock(TicketReplyReviserAssistant.class);
        OrderAgentService orderAgent = mock(OrderAgentService.class);
        RagQaService ragQa = mock(RagQaService.class);
        when(orderAgent.chat("查询订单 O1001")).thenReturn("O1001，399");
        when(reviewer.review("ORDER", "查询订单 O1001", "O1001，399"))
                .thenReturn("REVISE:表达不完整");
        when(reviser.revise("ORDER", "查询订单 O1001", "O1001，399", "表达不完整"))
                .thenReturn("订单 O1001 的金额为 399 元。");
        when(reviewer.review("ORDER", "查询订单 O1001", "订单 O1001 的金额为 399 元。"))
                .thenReturn("PASS");

        TicketWorkflowService service = service(
                classifier, reviewer, reviser, orderAgent, ragQa, 3);

        TicketWorkflowResult result = service.handle("查询订单 O1001");

        assertThat(result.status()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(result.reply()).isEqualTo("订单 O1001 的金额为 399 元。");
        assertThat(result.reviewIterations()).isEqualTo(2);
    }

    @Test
    void reachesHardLimitAndEscalatesInsteadOfLoopingForever() {
        TicketClassifierAssistant classifier = message -> "ORDER";
        TicketReplyReviewerAssistant reviewer = (category, question, draft) -> "REVISE:仍不符合规则";
        TicketReplyReviserAssistant reviser = (category, question, draft, feedback) -> draft + "（已修改）";
        OrderAgentService orderAgent = mock(OrderAgentService.class);
        RagQaService ragQa = mock(RagQaService.class);
        when(orderAgent.chat("查询订单 O1001")).thenReturn("订单 O1001");

        TicketWorkflowService service = service(
                classifier, reviewer, reviser, orderAgent, ragQa, 2);

        TicketWorkflowResult result = service.handle("查询订单 O1001");

        assertThat(result.status()).isEqualTo(WorkflowStatus.HUMAN_REQUIRED);
        assertThat(result.reviewIterations()).isEqualTo(2);
        assertThat(result.reason()).contains("最大复核次数");
        assertThat(result.reply()).isEqualTo("订单 O1001（已修改）");
    }

    private TicketWorkflowService service(TicketClassifierAssistant classifier,
                                          TicketReplyReviewerAssistant reviewer,
                                          TicketReplyReviserAssistant reviser,
                                          OrderAgentService orderAgent,
                                          RagQaService ragQa,
                                          int maxIterations) {
        return new TicketWorkflowService(
                classifier,
                reviewer,
                reviser,
                orderAgent,
                ragQa,
                new AgentWorkflowProperties(maxIterations)
        );
    }
}
