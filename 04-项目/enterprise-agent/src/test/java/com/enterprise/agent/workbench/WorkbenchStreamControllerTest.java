package com.enterprise.agent.workbench;

import com.enterprise.agent.chat.StreamingChatService;
import com.enterprise.agent.identity.DemoLoginRequest;
import com.enterprise.agent.identity.DemoLoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkbenchStreamControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private StreamingChatService model;

    @Test
    void validBearerReceivesNamedSseEventsButMissingBearerNeverCallsModel() throws Exception {
        mockMvc.perform(post("/api/v1/workbench/query/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"售后期限是什么？\",\"mode\":\"KNOWLEDGE\"}"))
                .andExpect(status().isUnauthorized());
        verify(model, never()).stream(anyString(), anyList(), any(), any(), any());

        String loginBody = mockMvc.perform(post("/api/v1/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DemoLoginRequest("service-a"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = objectMapper.readValue(loginBody, DemoLoginResponse.class).accessToken();

        doAnswer(invocation -> {
            Consumer<String> partial = invocation.getArgument(2);
            Runnable complete = invocation.getArgument(3);
            partial.accept("按规则[1]，7天内。");
            complete.run();
            return null;
        }).when(model).stream(anyString(), anyList(), any(), any(), any());

        MvcResult pending = mockMvc.perform(post("/api/v1/workbench/query/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"售后期限是什么？\",\"mode\":\"KNOWLEDGE\"}"))
                .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(pending))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:citations")))
                .andExpect(content().string(containsString("tenant-a-after-sales")))
                .andExpect(content().string(containsString("event:delta")))
                .andExpect(content().string(containsString("event:done")));
    }
}
