package com.enterprise.agent.supervisor;

import com.enterprise.agent.agent.OrderAgentService;
import com.enterprise.agent.rag.RagChatResponse;
import com.enterprise.agent.rag.RagQaService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupervisorToolsTest {

    @Test
    void handleOrderDelegatesToOrderAgent() {
        OrderAgentService orderAgentService = mock(OrderAgentService.class);
        when(orderAgentService.chat(any())).thenReturn("订单 O1001：金额 399.0 元");

        SupervisorTools tools = new SupervisorTools(orderAgentService, mock(RagQaService.class));
        String result = tools.handleOrder("查询订单 O1001");

        assertThat(result).contains("399.0");
        verify(orderAgentService).chat("查询订单 O1001");
    }

    @Test
    void handleKnowledgeDelegatesToRagAgent() {
        RagQaService ragQaService = mock(RagQaService.class);
        when(ragQaService.ask(eq("年假有几天？"), any(), any()))
                .thenReturn(new RagChatResponse("入职满一年享有 5 天年假。", List.of()));

        SupervisorTools tools = new SupervisorTools(mock(OrderAgentService.class), ragQaService);
        String result = tools.handleKnowledge("年假有几天？");

        assertThat(result).contains("5 天年假");
        verify(ragQaService).ask(eq("年假有几天？"), any(), any());
    }
}