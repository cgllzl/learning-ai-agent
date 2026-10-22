package com.enterprise.agent.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkbenchRouteController.class)
class WorkbenchRouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkbenchRoutingService routingService;

    @Test
    @DisplayName("目的：前端收到稳定路由契约，空白或超长问题在进入模型前被拒绝")
    void routeApiReturnsContractAndRejectsBlankQuestion() throws Exception {
        when(routingService.route("查询订单 O1001"))
                .thenReturn(new WorkbenchRouteResult(
                        TicketCategory.ORDER, WorkbenchView.ORDERS, false, "NOT_EXECUTED"));

        mockMvc.perform(post("/api/workbench/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"查询订单 O1001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("ORDER"))
                .andExpect(jsonPath("$.suggestedView").value("ORDERS"))
                .andExpect(jsonPath("$.requiresHumanReview").value(false))
                .andExpect(jsonPath("$.executionStatus").value("NOT_EXECUTED"));

        mockMvc.perform(post("/api/workbench/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_MESSAGE"));

        mockMvc.perform(post("/api/workbench/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_MESSAGE"));
    }
}
