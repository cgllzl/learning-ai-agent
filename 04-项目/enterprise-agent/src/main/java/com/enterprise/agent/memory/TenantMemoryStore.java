package com.enterprise.agent.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按租户隔离的记忆仓库（补全 Memory 模块）。
 * 每个租户一条独立 ChatMemory，保证 A 公司的上下文不会串到 B 公司。
 */
public class TenantMemoryStore {

    private final Map<String, ChatMemory> memories = new ConcurrentHashMap<>();

    public ChatMemory forTenant(String tenantId) {
        return memories.computeIfAbsent(tenantId, id ->
                MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(8)
                        .build());
    }
}
