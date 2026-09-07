package com.enterprise.agent.workflow.durable;

import java.time.Instant;
import java.util.List;

/**
 * 一次 Agent Workflow 的“游戏存档”。
 */
public record AgentRun(
        String runId,
        String userId,
        String tenantId,
        String idempotencyKey,
        String orderId,
        String newStatus,
        String originalStatus,
        List<AgentStep> plannedSteps,
        List<AgentStep> completedSteps,
        AgentStep nextStep,
        AgentRunStatus status,
        String approvalId,
        String lastError,
        Instant updatedAt
) {

    public AgentRun {
        plannedSteps = List.copyOf(plannedSteps);
        completedSteps = List.copyOf(completedSteps);
    }

    public AgentRun transition(AgentRunStatus newRunStatus,
                               AgentStep newNextStep,
                               List<AgentStep> newCompletedSteps,
                               String newLastError) {
        return new AgentRun(
                runId,
                userId,
                tenantId,
                idempotencyKey,
                orderId,
                newStatus,
                originalStatus,
                plannedSteps,
                newCompletedSteps,
                newNextStep,
                newRunStatus,
                approvalId,
                newLastError,
                Instant.now()
        );
    }
}
