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

@WebMvcTest(RagHybridSearchController.class)
class RagHybridSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HybridSearchService hybridSearchService;

    @Test
    void hybridSearchReturnsChunks() throws Exception {
        when(hybridSearchService.search(any(), any(), any(), any())).thenReturn(List.of(
                new RetrievedChunk("虚拟线程提升并发吞吐量", 0.92, "DOC1")));

        mockMvc.perform(post("/rag/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"什么是虚拟线程"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunks[0].documentId").value("DOC1"));
    }

    @Test
    void rejectsEmptyQuery() throws Exception {
        mockMvc.perform(post("/rag/hybrid-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}