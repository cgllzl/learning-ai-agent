package com.enterprise.agent.identity;

import com.enterprise.agent.workflow.TicketCategory;
import com.enterprise.agent.workflow.WorkbenchRouteResult;
import com.enterprise.agent.workflow.WorkbenchRoutingService;
import com.enterprise.agent.workflow.WorkbenchView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkbenchRoutingService routingService;

    @Test
    @DisplayName("目的：后端签发会话并恢复受信租户，伪造或缺失令牌不能访问工作台")
    void loginCreatesTrustedSessionAndProtectsWorkbench() throws Exception {
        mockMvc.perform(get("/api/v1/auth/demo-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.accountId == 'service-a')]").exists())
                .andExpect(jsonPath("$[?(@.accountId == 'service-b')]").exists());

        String loginBody = mockMvc.perform(post("/api/v1/auth/demo-login")
                        .header("Origin", "http://127.0.0.1:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"service-a\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
                .andExpect(jsonPath("$.principal.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.principal.roles[0]").value("CUSTOMER_SERVICE"))
                .andReturn().getResponse().getContentAsString();
        JsonNode login = objectMapper.readTree(loginBody);
        String token = login.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-service-a"))
                .andExpect(jsonPath("$.tenantId").value("tenant-a"));

        mockMvc.perform(post("/api/v1/workbench/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"查询订单 O1001\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/workbench/route")
                        .header("Authorization", "Bearer forged-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"查询订单 O1001\"}"))
                .andExpect(status().isUnauthorized());

        // 未认证和伪造令牌都会在安全过滤链被拦截，不能进入工作台业务服务。
        verify(routingService, never()).route(anyString());

        when(routingService.route("查询订单 O1001"))
                .thenReturn(new WorkbenchRouteResult(
                        TicketCategory.ORDER, WorkbenchView.ORDERS, false, "NOT_EXECUTED"));
        mockMvc.perform(post("/api/v1/workbench/route")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"查询订单 O1001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedView").value("ORDERS"));

        // 默认 verify 等价于 times(1)：证明合法请求确实只进入了一次业务服务。
        verify(routingService).route("查询订单 O1001");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
