package com.enterprise.agent.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Week 5 Day 5 的大模型例子：真实调用 DeepSeek，同时验证密钥在配置摘要中不泄露。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test SecretManagementLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class SecretManagementLiveTest {

    @Test
    void modelWorksAndApiKeyIsMaskedInSummary() {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        SecretSafeChatService service = new SecretSafeChatService(apiKey);

        // 1) 配置摘要只包含脱敏后的密钥
        String summary = service.safeConfigSummary();
        System.out.println("[安全配置摘要] " + summary);
        assertThat(summary).contains("****");
        assertThat(summary).doesNotContain(apiKey);

        // 2) 密钥包装对象的 toString 也不泄露明文
        assertThat(SecretValue.of(apiKey).toString()).doesNotContain(apiKey);

        // 3) 真实调用 DeepSeek，证明脱敏不影响正常使用
        String reply = service.chat("你是一个友好的助手", "请用一句话介绍你自己");
        System.out.println("[Secret 管理回答] " + reply);
        assertThat(reply).isNotBlank();
    }
}
