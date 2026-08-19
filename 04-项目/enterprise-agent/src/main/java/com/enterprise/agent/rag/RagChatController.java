package com.enterprise.agent.rag;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag")
public class RagChatController {

    private final RagQaService ragQaService;

    public RagChatController(RagQaService ragQaService) {
        this.ragQaService = ragQaService;
    }

    @PostMapping("/chat")
    public RagChatResponse chat(@Valid @RequestBody RagChatRequest request) {
        return ragQaService.ask(request.question(), request.documentId(), request.maxResults());
    }
}