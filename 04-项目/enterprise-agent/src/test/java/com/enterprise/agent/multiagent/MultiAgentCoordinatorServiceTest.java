package com.enterprise.agent.multiagent;

import com.enterprise.agent.agent.OrderAgentService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiAgentCoordinatorServiceTest {

    @Test
    void passesOrderFactsAsStateToReplyAgent() {
        OrderAgentService orderAgentService = mock(OrderAgentService.class);
        CustomerReplyService customerReplyService = mock(CustomerReplyService.class);

        String question = "查询订单 O1001 的信息";
        String orderFacts = "订单 O1001：用户 U1，商品 P1，金额 399.0 元，状态 PAID";
        when(orderAgentService.chat(question)).thenReturn(orderFacts);
        when(customerReplyService.compose(question, orderFacts))
                .thenReturn("您好，您查询的订单 O1001 金额为 399.0 元。");

        MultiAgentCoordinatorService coordinator =
                new MultiAgentCoordinatorService(orderAgentService, customerReplyService);

        String reply = coordinator.handleCustomerQuestion(question);

        assertThat(reply).contains("O1001").contains("399");
        verify(orderAgentService).chat(question);
        verify(customerReplyService).compose(question, orderFacts);
    }
}
