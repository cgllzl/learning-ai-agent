package com.enterprise.agent.workflow.durable;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 内存版 Checkpoint Store：演示存档、恢复、租户隔离、删除和过期清理语义。
 */
@Component
public class InMemoryAgentCheckpointStore implements AgentCheckpointStore {

    private final Map<String, AgentRun> runs = new HashMap<>();
    private final Map<String, String> idempotencyIndex = new HashMap<>();

    @Override
    public synchronized void save(AgentRun run) {
        AgentRun existingRun = runs.get(run.runId());
        if (existingRun != null && !existingRun.tenantId().equals(run.tenantId())) {
            throw new SecurityException("Agent Run 不属于当前租户");
        }

        String indexKey = indexKey(run.tenantId(), run.idempotencyKey());
        String boundRunId = idempotencyIndex.get(indexKey);
        if (boundRunId != null && !boundRunId.equals(run.runId())) {
            throw new IllegalStateException("幂等键已绑定到另一个 Agent Run");
        }

        runs.put(run.runId(), run);
        idempotencyIndex.put(indexKey, run.runId());
    }

    @Override
    public synchronized Optional<AgentRun> load(String tenantId, String runId) {
        return Optional.ofNullable(runs.get(runId))
                .filter(run -> run.tenantId().equals(tenantId));
    }

    @Override
    public synchronized Optional<AgentRun> findByIdempotencyKey(String tenantId, String idempotencyKey) {
        String runId = idempotencyIndex.get(indexKey(tenantId, idempotencyKey));
        return runId == null ? Optional.empty() : load(tenantId, runId);
    }

    @Override
    public synchronized boolean delete(String tenantId, String runId) {
        AgentRun run = load(tenantId, runId).orElse(null);
        if (run == null) {
            return false;
        }
        runs.remove(runId);
        idempotencyIndex.remove(indexKey(tenantId, run.idempotencyKey()));
        return true;
    }

    @Override
    public synchronized int deleteUpdatedBefore(String tenantId, Instant cutoff) {
        var expiredRunIds = runs.values().stream()
                .filter(run -> run.tenantId().equals(tenantId))
                .filter(run -> run.updatedAt().isBefore(cutoff))
                .map(AgentRun::runId)
                .toList();
        expiredRunIds.forEach(runId -> delete(tenantId, runId));
        return expiredRunIds.size();
    }

    private String indexKey(String tenantId, String idempotencyKey) {
        return tenantId + "\u0000" + idempotencyKey;
    }
}
