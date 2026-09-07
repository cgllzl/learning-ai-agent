package com.enterprise.agent.workflow.durable;

public enum AgentRunStatus {
    WAITING_APPROVAL,
    RUNNING,
    FAILED,
    COMPLETED,
    COMPENSATED,
    COMPENSATION_FAILED
}
