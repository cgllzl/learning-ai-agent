package com.enterprise.agent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderAgentController.class)
class OrderAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderAgentService orderAgentService;

    @Test
    void orderReturnsReply() throws Exception {
        when(orderAgentService.chat(anyString())).thenReturn("订单 O1001：用户 U1，商品 P1，金额 399.0 元，状态 PAID");

        mockMvc.perform(post("/agent/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"查询订单 O1001 的信息"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("订单 O1001：用户 U1，商品 P1，金额 399.0 元，状态 PAID"));
    }

    @Test
    void orderRejectsEmptyMessage() throws Exception {
        mockMvc.perform(post("/agent/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}