package com.enterprise.agent.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 多轮记忆示例：把历史消息拼进请求，并把本轮问答写回记忆。
 */
public class MemoryDemoService {

    private static final String SYSTEM_PROMPT = """
            你是企业客服助手。回答时优先使用对话历史中的信息。
            如果用户问之前说过的事情，请基于历史回答。""";

    private final OpenAiChatModel chatModel;
    private final TenantMemoryStore memoryStore;

    public MemoryDemoService(OpenAiChatModel chatModel, TenantMemoryStore memoryStore) {
        this.chatModel = chatModel;
        this.memoryStore = memoryStore;
    }

    public String chat(String tenantId, String userMessage) {
        ChatMemory memory = memoryStore.forTenant(tenantId);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM_PROMPT));
        messages.addAll(memory.messages());
        messages.add(UserMessage.from(userMessage));

        ChatResponse response = chatModel.chat(messages);
        String reply = response.aiMessage().text();

        memory.add(UserMessage.from(userMessage), AiMessage.from(reply));
        return reply;
    }
}
