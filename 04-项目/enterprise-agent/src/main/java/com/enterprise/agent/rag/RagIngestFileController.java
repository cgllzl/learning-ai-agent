package com.enterprise.agent.rag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 文件上传入库（Week 3 补充）：POST /rag/ingest/file，支持 txt / md。
 */
@RestController
@RequestMapping("/rag")
public class RagIngestFileController {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt", "md", "markdown");

    private final DocumentIngestionService ingestionService;

    public RagIngestFileController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(value = "/ingest/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestionResult ingestFile(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "documentId", required = false) String documentId)
            throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = extensionOf(originalName);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension + "，仅支持 txt / md");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件内容为空");
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String effectiveDocumentId = (documentId == null || documentId.isBlank())
                ? deriveDocumentId(originalName)
                : documentId;

        return ingestionService.ingest(effectiveDocumentId, content, Map.of("fileName", originalName));
    }

    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String deriveDocumentId(String fileName) {
        if (fileName == null) {
            return "upload";
        }
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        return base.isBlank() ? "upload" : base;
    }
}