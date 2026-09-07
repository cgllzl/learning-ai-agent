package com.enterprise.agent.workflow.durable;

import java.time.Instant;
import java.util.Optional;

/**
 * Agent Run 的持久化边界。学习期用内存实现，生产环境可替换为 MySQL/Redis。
 */
public interface AgentCheckpointStore {

    void save(AgentRun run);

    Optional<AgentRun> load(String tenantId, String runId);

    Optional<AgentRun> findByIdempotencyKey(String tenantId, String idempotencyKey);

    boolean delete(String tenantId, String runId);

    int deleteUpdatedBefore(String tenantId, Instant cutoff);
}
