package com.enterprise.agent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagChatController.class)
class RagChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RagQaService ragQaService;

    @Test
    void chatReturnsAnswerAndSources() throws Exception {
        when(ragQaService.ask(any(), any(), any())).thenReturn(new RagChatResponse(
                "根据 [1]，虚拟线程可以提升并发吞吐量。",
                List.of(new RetrievedChunk("虚拟线程提升并发吞吐量", 0.95, "DOC1"))));

        mockMvc.perform(post("/rag/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"虚拟线程有什么用？"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("根据 [1]，虚拟线程可以提升并发吞吐量。"))
                .andExpect(jsonPath("$.sources[0].documentId").value("DOC1"));
    }

    @Test
    void chatRejectsEmptyQuestion() throws Exception {
        mockMvc.perform(post("/rag/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}