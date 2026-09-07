package com.enterprise.agent.workflow.durable;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 给有副作用的工具套上“快递单号”：同一租户、同一幂等键只真正执行一次。
 */
@Component
public class IdempotentToolExecutor {

    private final Map<String, StoredExecution> executions = new HashMap<>();

    public synchronized ToolExecutionResult execute(String tenantId,
                                                    String idempotencyKey,
                                                    Supplier<String> action) {
        String key = key(tenantId, idempotencyKey);
        StoredExecution existing = executions.get(key);
        if (existing != null) {
            return new ToolExecutionResult(existing.result(), true, existing.executedAt());
        }

        String result = action.get();
        StoredExecution stored = new StoredExecution(result, Instant.now());
        executions.put(key, stored);
        return new ToolExecutionResult(stored.result(), false, stored.executedAt());
    }

    public synchronized int executionCount(String tenantId) {
        String prefix = tenantId + "\u0000";
        return (int) executions.keySet().stream().filter(key -> key.startsWith(prefix)).count();
    }

    private String key(String tenantId, String idempotencyKey) {
        return tenantId + "\u0000" + idempotencyKey;
    }

    private record StoredExecution(String result, Instant executedAt) {
    }
}
