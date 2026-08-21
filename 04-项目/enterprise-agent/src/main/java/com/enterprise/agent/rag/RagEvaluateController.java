package com.enterprise.agent.rag;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag")
public class RagEvaluateController {

    private final RagEvaluationService evaluationService;

    public RagEvaluateController(RagEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/evaluate")
    public RagEvalMetrics evaluate(@Valid @RequestBody RagEvaluateRequest request) {
        return evaluationService.evaluate(request.cases());
    }
}