package com.enterprise.agent.workflow;

import java.util.Locale;

/**
 * 工单的确定性路由结果。
 */
public enum TicketCategory {
    ORDER,
    KNOWLEDGE,
    HUMAN;

    /**
     * 模型输出必须严格等于一个允许值；含糊、复合或额外输出一律转人工。
     */
    public static TicketCategory fromModelOutput(String output) {
        if (output == null) {
            return HUMAN;
        }
        String normalized = output.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ORDER" -> ORDER;
            case "KNOWLEDGE" -> KNOWLEDGE;
            case "HUMAN" -> HUMAN;
            default -> HUMAN;
        };
    }
}
