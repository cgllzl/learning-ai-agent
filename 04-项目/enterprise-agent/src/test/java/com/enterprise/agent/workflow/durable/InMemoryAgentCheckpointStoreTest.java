package com.enterprise.agent.workflow.durable;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAgentCheckpointStoreTest {

    @Test
    void loadsRunOnlyInsideOwningTenant() {
        InMemoryAgentCheckpointStore store = new InMemoryAgentCheckpointStore();
        store.save(run("run-1", "tenant-a", "key-1", Instant.now()));

        assertThat(store.load("tenant-a", "run-1")).isPresent();
        assertThat(store.load("tenant-b", "run-1")).isEmpty();
    }

    @Test
    void findsByIdempotencyKeyAndRemovesItsIndexOnDelete() {
        InMemoryAgentCheckpointStore store = new InMemoryAgentCheckpointStore();
        store.save(run("run-1", "tenant-a", "key-1", Instant.now()));

        assertThat(store.findByIdempotencyKey("tenant-a", "key-1"))
                .map(AgentRun::runId)
                .contains("run-1");
        assertThat(store.delete("tenant-a", "run-1")).isTrue();
        assertThat(store.findByIdempotencyKey("tenant-a", "key-1")).isEmpty();
    }

    @Test
    void expiryCleanupDoesNotDeleteAnotherTenantsRun() {
        InMemoryAgentCheckpointStore store = new InMemoryAgentCheckpointStore();
        Instant old = Instant.parse("2026-01-01T00:00:00Z");
        store.save(run("run-a", "tenant-a", "key-a", old));
        store.save(run("run-b", "tenant-b", "key-b", old));

        int deleted = store.deleteUpdatedBefore(
                "tenant-a", Instant.parse("2026-02-01T00:00:00Z"));

        assertThat(deleted).isEqualTo(1);
        assertThat(store.load("tenant-a", "run-a")).isEmpty();
        assertThat(store.load("tenant-b", "run-b")).isPresent();
    }

    private AgentRun run(String runId, String tenantId, String idempotencyKey, Instant updatedAt) {
        return new AgentRun(
                runId,
                "user-1",
                tenantId,
                idempotencyKey,
                "O1003",
                "SHIPPED",
                "PENDING",
                List.of(AgentStep.UPDATE_ORDER, AgentStep.SEND_NOTIFICATION),
                List.of(),
                AgentStep.UPDATE_ORDER,
                AgentRunStatus.WAITING_APPROVAL,
                "approval-1",
                null,
                updatedAt
        );
    }
}
