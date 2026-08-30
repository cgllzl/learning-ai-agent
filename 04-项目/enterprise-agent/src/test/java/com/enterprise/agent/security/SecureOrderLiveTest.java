package com.enterprise.agent.security;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Week 5 Day 2 的大模型例子：RBAC 拒绝无权用户；租户隔离保证同号不同数据。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test SecureOrderLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class SecureOrderLiveTest {

    @Test
    void rbacAndTenantIsolationWorkWithRealModel() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        SecureOrderAgentService agent = new SecureOrderAgentService(model, new RbacService());

        // 1) 租户 t1 的客服查 O1001：只能看到 t1 的数据（399）
        SecuritySubject t1Cs = new SecuritySubject("u1", "t1", Set.of("CUSTOMER_SERVICE"));
        String t1Reply = agent.chat(t1Cs, "查询订单 O1001 的信息");
        System.out.println("[租户 t1 回答] " + t1Reply);
        assertThat(t1Reply).contains("399");

        // 2) 租户 t2 的客服查同一个 O1001：只能看到 t2 的数据（1299）
        SecuritySubject t2Cs = new SecuritySubject("u2", "t2", Set.of("CUSTOMER_SERVICE"));
        String t2Reply = agent.chat(t2Cs, "查询订单 O1001 的信息");
        System.out.println("[租户 t2 回答] " + t2Reply);
        assertThat(t2Reply).contains("1299");

        // 3) 无权限角色：直接抛异常，不会走到大模型
        SecuritySubject employee = new SecuritySubject("u3", "t1", Set.of("EMPLOYEE"));
        assertThatThrownBy(() -> agent.chat(employee, "查询订单 O1001 的信息"))
                .isInstanceOf(AgentAccessDeniedException.class);
    }
}
