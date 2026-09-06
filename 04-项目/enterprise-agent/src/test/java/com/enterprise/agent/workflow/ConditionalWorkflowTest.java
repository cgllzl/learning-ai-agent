package com.enterprise.agent.workflow;

import com.enterprise.agent.agent.OrderAgentService;
import com.enterprise.agent.rag.RagChatResponse;
import com.enterprise.agent.rag.RagQaService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConditionalWorkflowTest {

    @Test
    void routesOrderQuestionAndStopsWhenReviewPasses() {
        TicketClassifierAssistant classifier = message -> "ORDER";
        TicketReplyReviewerAssistant reviewer = (category, question, draft) -> "PASS";
        TicketReplyReviserAssistant reviser = mock(TicketReplyReviserAssistant.class);
        OrderAgentService orderAgent = mock(OrderAgentService.class);
        RagQaService ragQa = mock(RagQaService.class);
        when(orderAgent.chat("查询订单 O1001")).thenReturn("订单 O1001 金额 399 元");

        TicketWorkflowService service = service(
                classifier, reviewer, reviser, orderAgent, ragQa, 3);

        TicketWorkflowResult result = service.handle("查询订单 O1001");

        assertThat(result.category()).isEqualTo(TicketCategory.ORDER);
        assertThat(result.status()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(result.reply()).contains("O1001").contains("399");
        assertThat(result.reviewIterations()).isEqualTo(1);
        verify(orderAgent).chat("查询订单 O1001");
        verify(ragQa, never()).ask(anyString(), any(), anyInt());
        verify(reviser, never()).revise(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void routesKnowledgeQuestionToRagBranch() {
        TicketClassifierAssistant classifier = message -> "KNOWLEDGE";
        TicketReplyReviewerAssistant reviewer = (category, question, draft) -> "PASS";
        TicketReplyReviserAssistant reviser = mock(TicketReplyReviserAssistant.class);
        OrderAgentService orderAgent = mock(OrderAgentService.class);
        RagQaService ragQa = mock(RagQaService.class);
        when(ragQa.ask("公司的报销制度是什么", null, 5))
                .thenReturn(new RagChatResponse("差旅报销需在 30 天内提交 [1]", List.of()));

        TicketWorkflowService service = service(
                classifier, reviewer, reviser, orderAgent, ragQa, 3);

        TicketWorkflowResult result = service.handle("公司的报销制度是什么");

        assertThat(result.category()).isEqualTo(TicketCategory.KNOWLEDGE);
        assertThat(result.status()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(result.reply()).contains("30 天").contains("[1]");
        verify(orderAgent, never()).chat(anyString());
    }

    @Test
    void ambiguousClassificationFailsClosedToHuman() {
        TicketClassifierAssistant classifier = message -> "ORDER or KNOWLEDGE";
        TicketReplyReviewerAssistant reviewer = mock(TicketReplyReviewerAssistant.class);
        TicketReplyReviserAssistant reviser = mock(TicketReplyReviserAssistant.class);
        OrderAgentService orderAgent = mock(OrderAgentService.class);
        RagQaService ragQa = mock(RagQaService.class);

        TicketWorkflowService service = service(
                classifier, reviewer, reviser, orderAgent, ragQa, 3);

        TicketWorkflowResult result = service.handle("帮我处理一下这个问题");

        assertThat(result.category()).isEqualTo(TicketCategory.HUMAN);
        assertThat(result.status()).isEqualTo(WorkflowStatus.HUMAN_REQUIRED);
        assertThat(result.reviewIterations()).isZero();
        verify(orderAgent, never()).chat(anyString());
        verify(ragQa, never()).ask(anyString(), any(), anyInt());
        verify(reviewer, never()).review(anyString(), anyString(), anyString());
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
