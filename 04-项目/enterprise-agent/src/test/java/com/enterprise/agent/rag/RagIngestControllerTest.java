package com.enterprise.agent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagIngestController.class)
class RagIngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentIngestionService ingestionService;

    @Test
    void ingestReturnsResult() throws Exception {
        when(ingestionService.ingest(eq("DOC1"), any(), any()))
                .thenReturn(new IngestionResult("DOC1", 3, List.of("id1", "id2", "id3")));

        mockMvc.perform(post("/rag/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentId":"DOC1","content":"这是一段需要入库的文档内容"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value("DOC1"))
                .andExpect(jsonPath("$.segmentCount").value(3));
    }

    @Test
    void ingestRejectsEmptyContent() throws Exception {
        mockMvc.perform(post("/rag/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentId":"DOC1","content":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}