package com.enterprise.agent.security;

import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderTools;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Week 5 Day 3 的大模型例子：模型只能看到当前角色有权使用的工具。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test PermissionAwareOrderLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class PermissionAwareOrderLiveTest {

    @Test
    void customerServiceCannotMutateButAdminCan() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        MockOrderData data = new MockOrderData();
        PermissionAwareOrderAgentService agent =
                new PermissionAwareOrderAgentService(model, new OrderTools(data));

        SecuritySubject customerService = new SecuritySubject("u1", "t1", Set.of("CUSTOMER_SERVICE"));
        SecuritySubject orderAdmin = new SecuritySubject("u2", "t1", Set.of("ORDER_ADMIN"));

        // 1) 客服尝试改订单状态：没有 updateOrderStatus 工具，数据不会被改
        String customerReply = agent.chat(customerService, "把订单 O1003 的状态改为 SHIPPED");
        System.out.println("[客服尝试改状态] " + customerReply);
        assertThat(data.findOrderById("O1003").orElseThrow().status()).isEqualTo("PENDING");

        // 2) 客服可以正常查订单
        String readReply = agent.chat(customerService, "查询订单 O1001 的信息");
        System.out.println("[客服查询订单] " + readReply);
        assertThat(readReply).contains("O1001").contains("399");

        // 3) 管理员可以改订单状态
        String adminReply = agent.chat(orderAdmin, "把订单 O1003 的状态改为 SHIPPED");
        System.out.println("[管理员改状态] " + adminReply);
        assertThat(data.findOrderById("O1003").orElseThrow().status()).isEqualTo("SHIPPED");
    }
}
