package com.enterprise.agent.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @MockBean
    private StreamingChatService streamingChatService;

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

    @Test
    void chatStreamReturnsSseEvents() throws Exception {
        doAnswer(invocation -> {
            Consumer<String> onPartial = invocation.getArgument(2);
            Runnable onComplete = invocation.getArgument(3);
            onPartial.accept("你");
            onPartial.accept("好");
            onComplete.run();
            return null;
        }).when(streamingChatService)
                .stream(eq(null), anyList(), any(), any(), any());

        MvcResult result = mockMvc.perform(post("/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages":[{"role":"user","content":"你好"}]}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(allOf(
                        containsString("data:你"),
                        containsString("data:好"),
                        containsString("data:[DONE]"))));
    }

    @Test
    void chatStreamRejectsEmptyMessages() throws Exception {
        mockMvc.perform(post("/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messages\":[]}"))
                .andExpect(status().isBadRequest());
    }
}