package com.enterprise.agent.memory;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Memory 模块的大模型例子：多轮记忆 + 多租户记忆隔离。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test MemoryLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class MemoryLiveTest {

    @Test
    void remembersWithinTenantAndIsolatesAcrossTenants() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        MemoryDemoService service = new MemoryDemoService(model, new TenantMemoryStore());

        String t1First = service.chat("t1", "我的订单号是 O1001");
        System.out.println("[t1 第一轮] " + t1First);
        String t1Second = service.chat("t1", "我的订单号是多少？");
        System.out.println("[t1 第二轮] " + t1Second);
        assertThat(t1Second).contains("O1001");

        String t2First = service.chat("t2", "我的订单号是 O2001");
        String t2Second = service.chat("t2", "我的订单号是多少？");
        System.out.println("[t2 第二轮] " + t2Second);
        assertThat(t2Second).contains("O2001");
    }
}
