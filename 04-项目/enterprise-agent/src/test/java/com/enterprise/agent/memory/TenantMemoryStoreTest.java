package com.enterprise.agent.memory;

import dev.langchain4j.memory.ChatMemory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMemoryStoreTest {

    @Test
    void eachTenantGetsIndependentMemory() {
        TenantMemoryStore store = new TenantMemoryStore();

        ChatMemory t1 = store.forTenant("t1");
        ChatMemory t1Again = store.forTenant("t1");
        ChatMemory t2 = store.forTenant("t2");

        assertThat(t1).isSameAs(t1Again);
        assertThat(t1).isNotSameAs(t2);
    }
}
