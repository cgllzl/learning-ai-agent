package com.enterprise.agent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagEvaluateController.class)
class RagEvaluateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RagEvaluationService evaluationService;

    @Test
    void evaluateReturnsMetrics() throws Exception {
        when(evaluationService.evaluate(anyList())).thenReturn(
                RagEvaluator.metrics(2, 2, 2, 1.0));

        mockMvc.perform(post("/rag/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cases":[
                                  {"question":"年假有几天？","expectedDocumentId":"HR-001"},
                                  {"question":"报销怎么报？","expectedDocumentId":"FIN-001"}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.recallRate").value(1.0))
                .andExpect(jsonPath("$.citationPrecision").value(1.0));
    }

    @Test
    void evaluateRejectsEmptyCases() throws Exception {
        mockMvc.perform(post("/rag/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cases":[]}
                                """))
                .andExpect(status().isBadRequest());
    }
}