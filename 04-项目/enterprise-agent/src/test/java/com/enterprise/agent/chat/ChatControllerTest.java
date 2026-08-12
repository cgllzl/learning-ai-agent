package com.enterprise.agent.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Test
    void chatReturnsReply() throws Exception {
        when(chatService.chat(eq(null), any())).thenReturn("你好，我是企业助手");

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages":[{"role":"user","content":"你好"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("你好，我是企业助手"));
    }

    @Test
    void supportsSystemPrompt() throws Exception {
        when(chatService.chat(eq("你是企业助手"), any())).thenReturn("好的");

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "systemPrompt": "你是企业助手",
                                  "messages": [{"role": "user", "content": "你好"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("好的"));
    }

    @Test
    void emptyMessagesReturns400() throws Exception {
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messages\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownRoleReturns400() throws Exception {
        when(chatService.chat(eq(null), any()))
                .thenThrow(new IllegalArgumentException("不支持的 role: robot"));

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages":[{"role":"robot","content":"hi"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("不支持的 role: robot"));
    }
}