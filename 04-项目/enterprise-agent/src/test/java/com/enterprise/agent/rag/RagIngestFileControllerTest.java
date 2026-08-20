package com.enterprise.agent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagIngestFileController.class)
class RagIngestFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentIngestionService ingestionService;

    @Test
    void uploadsTxtFileWithDerivedDocumentId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "年假制度.txt", "text/plain",
                "入职满一年享有 5 天年假。".getBytes(StandardCharsets.UTF_8));
        when(ingestionService.ingest(eq("年假制度"), any(), anyMap()))
                .thenReturn(new IngestionResult("年假制度", 1, List.of("id1")));

        mockMvc.perform(multipart("/rag/ingest/file").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value("年假制度"))
                .andExpect(jsonPath("$.segmentCount").value(1));

        verify(ingestionService).ingest(eq("年假制度"), eq("入职满一年享有 5 天年假。"), anyMap());
    }

    @Test
    void uploadsMarkdownWithExplicitDocumentId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.md", "text/markdown",
                "# 标题\n内容".getBytes(StandardCharsets.UTF_8));
        when(ingestionService.ingest(eq("HR-002"), any(), anyMap()))
                .thenReturn(new IngestionResult("HR-002", 1, List.of("id2")));

        mockMvc.perform(multipart("/rag/ingest/file")
                        .file(file)
                        .param("documentId", "HR-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value("HR-002"));
    }

    @Test
    void rejectsUnsupportedFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/rag/ingest/file").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("不支持的文件类型")));
    }

    @Test
    void rejectsEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/rag/ingest/file").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("文件内容为空")));
    }
}