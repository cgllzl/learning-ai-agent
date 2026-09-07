package com.enterprise.agent.workflow.durable;

import java.time.Instant;

public record ToolExecutionResult(String result, boolean replayed, Instant executedAt) {
}
