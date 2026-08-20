package com.enterprise.agent.rag;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag")
public class RagHybridSearchController {

    private final HybridSearchService hybridSearchService;

    public RagHybridSearchController(HybridSearchService hybridSearchService) {
        this.hybridSearchService = hybridSearchService;
    }

    @PostMapping("/hybrid-search")
    public RetrievalResult search(@Valid @RequestBody RetrievalRequest request) {
        return new RetrievalResult(hybridSearchService.search(
                request.query(), request.documentId(), request.maxResults(), request.minScore()));
    }
}