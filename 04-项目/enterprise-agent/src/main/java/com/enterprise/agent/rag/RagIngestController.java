package com.enterprise.agent.rag;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag")
public class RagIngestController {

    private final DocumentIngestionService ingestionService;

    public RagIngestController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public IngestionResult ingest(@Valid @RequestBody IngestionRequest request) {
        return ingestionService.ingest(request.documentId(), request.content(), request.metadata());
    }
}