package com.enterprise.agent.workflow;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 模型只提供候选类别；Java 将其收敛为三个固定页面建议，异常输出转人工。
 */
@Service
public class WorkbenchRoutingService {

    private static final String NOT_EXECUTED = "NOT_EXECUTED";

    private final TicketClassifierAssistant classifier;

    // 存在正式与测试两个构造器，明确告诉 Spring 正式运行时注入哪个。
    @Autowired
    public WorkbenchRoutingService(@Qualifier("openAiChatModel") OpenAiChatModel model) {
        // AiServices.build() 创建接口代理；真正调用模型发生在 classifier.classify(...)。
        this(AiServices.builder(TicketClassifierAssistant.class)
                .chatModel(model)
                .build());
    }

    WorkbenchRoutingService(TicketClassifierAssistant classifier) {
        this.classifier = classifier;
    }

    public WorkbenchRouteResult route(String message) {
        if (message == null || message.isBlank()) {
            return humanRoute();
        }

        TicketCategory category = TicketCategory.fromModelOutput(classifier.classify(message));
        return switch (category) {
            case ORDER -> new WorkbenchRouteResult(
                    category, WorkbenchView.ORDERS, false, NOT_EXECUTED);
            case KNOWLEDGE -> new WorkbenchRouteResult(
                    category, WorkbenchView.KNOWLEDGE, false, NOT_EXECUTED);
            case HUMAN -> humanRoute();
        };
    }

    private WorkbenchRouteResult humanRoute() {
        return new WorkbenchRouteResult(
                TicketCategory.HUMAN, WorkbenchView.HUMAN_QUEUE, true, NOT_EXECUTED);
    }
}
