package com.enterprise.agent.workflow.durable;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotentToolExecutorTest {

    @Test
    void sameTenantAndKeyExecutesSideEffectOnlyOnce() {
        IdempotentToolExecutor executor = new IdempotentToolExecutor();
        AtomicInteger sideEffects = new AtomicInteger();

        ToolExecutionResult first = executor.execute(
                "tenant-a", "run-1:UPDATE", () -> "count=" + sideEffects.incrementAndGet());
        ToolExecutionResult second = executor.execute(
                "tenant-a", "run-1:UPDATE", () -> "count=" + sideEffects.incrementAndGet());

        assertThat(sideEffects).hasValue(1);
        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.result()).isEqualTo(first.result());
    }

    @Test
    void sameKeyInDifferentTenantsIsIndependent() {
        IdempotentToolExecutor executor = new IdempotentToolExecutor();
        AtomicInteger sideEffects = new AtomicInteger();

        executor.execute("tenant-a", "same-key", () -> "a-" + sideEffects.incrementAndGet());
        executor.execute("tenant-b", "same-key", () -> "b-" + sideEffects.incrementAndGet());

        assertThat(sideEffects).hasValue(2);
    }

    @Test
    void failedExecutionIsNotCachedAndCanBeRetried() {
        IdempotentToolExecutor executor = new IdempotentToolExecutor();
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> executor.execute("tenant-a", "notify", () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("通知系统暂时不可用");
        })).isInstanceOf(IllegalStateException.class);

        ToolExecutionResult retried = executor.execute(
                "tenant-a", "notify", () -> "sent-" + attempts.incrementAndGet());

        assertThat(attempts).hasValue(2);
        assertThat(retried.replayed()).isFalse();
    }
}
