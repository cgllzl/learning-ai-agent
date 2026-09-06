package com.enterprise.agent.workflow;

/**
 * 对外只暴露可审计状态，不保存或要求模型输出隐藏思维过程。
 */
public record TicketWorkflowResult(
        TicketCategory category,
        WorkflowStatus status,
        String reply,
        int reviewIterations,
        String reason
) {
}
