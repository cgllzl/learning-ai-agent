package com.enterprise.agent.rag;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag")
public class RagRetrieveController {

    private final DocumentRetrievalService retrievalService;

    public RagRetrieveController(DocumentRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @PostMapping("/search")
    public RetrievalResult search(@Valid @RequestBody RetrievalRequest request) {
        return new RetrievalResult(retrievalService.retrieve(
                request.query(), request.documentId(), request.maxResults(), request.minScore()));
    }
}