package com.enterprise.agent.workflow;

import com.enterprise.agent.agent.OrderAgentService;
import com.enterprise.agent.rag.RagQaService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Week 6.5 Day 1：企业工单的 Conditional Workflow + 有限 Loop。
 *
 * 大模型负责分类、复核和文案修改；Java 代码负责允许的分支、循环上限和转人工。
 */
@Service
public class TicketWorkflowService {

    private static final String HUMAN_REPLY = "该请求需要人工客服确认，系统未执行任何业务操作。";

    private final TicketClassifierAssistant classifier;
    private final TicketReplyReviewerAssistant reviewer;
    private final TicketReplyReviserAssistant reviser;
    private final OrderAgentService orderAgentService;
    private final RagQaService ragQaService;
    private final int maxReviewIterations;

    @Autowired
    public TicketWorkflowService(@Qualifier("openAiChatModel") OpenAiChatModel chatModel,
                                 OrderAgentService orderAgentService,
                                 RagQaService ragQaService,
                                 AgentWorkflowProperties properties) {
        this(
                AiServices.builder(TicketClassifierAssistant.class)
                        .chatModel(chatModel)
                        .build(),
                AiServices.builder(TicketReplyReviewerAssistant.class)
                        .chatModel(chatModel)
                        .build(),
                AiServices.builder(TicketReplyReviserAssistant.class)
                        .chatModel(chatModel)
                        .build(),
                orderAgentService,
                ragQaService,
                properties
        );
    }

    TicketWorkflowService(TicketClassifierAssistant classifier,
                          TicketReplyReviewerAssistant reviewer,
                          TicketReplyReviserAssistant reviser,
                          OrderAgentService orderAgentService,
                          RagQaService ragQaService,
                          AgentWorkflowProperties properties) {
        this.classifier = classifier;
        this.reviewer = reviewer;
        this.reviser = reviser;
        this.orderAgentService = orderAgentService;
        this.ragQaService = ragQaService;
        this.maxReviewIterations = properties.maxReviewIterations();
    }

    public TicketWorkflowResult handle(String message) {
        TicketCategory category = TicketCategory.fromModelOutput(classifier.classify(message));
        if (category == TicketCategory.HUMAN) {
            return new TicketWorkflowResult(
                    category,
                    WorkflowStatus.HUMAN_REQUIRED,
                    HUMAN_REPLY,
                    0,
                    "意图含糊或属于高风险操作"
            );
        }

        String draft = switch (category) {
            case ORDER -> orderAgentService.chat(message);
            case KNOWLEDGE -> ragQaService.ask(message, null, 5).answer();
            case HUMAN -> throw new IllegalStateException("HUMAN 分支已提前返回");
        };

        for (int iteration = 1; iteration <= maxReviewIterations; iteration++) {
            ReviewDecision decision = ReviewDecision.fromModelOutput(
                    reviewer.review(category.name(), message, draft));
            if (decision.passed()) {
                return new TicketWorkflowResult(
                        category,
                        WorkflowStatus.COMPLETED,
                        draft,
                        iteration,
                        "质量复核通过"
                );
            }
            if (iteration < maxReviewIterations) {
                draft = reviser.revise(category.name(), message, draft, decision.feedback());
            }
        }

        return new TicketWorkflowResult(
                category,
                WorkflowStatus.HUMAN_REQUIRED,
                draft,
                maxReviewIterations,
                "达到最大复核次数，转人工处理"
        );
    }
}
