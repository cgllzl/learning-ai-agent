package com.enterprise.agent.security;

import com.enterprise.agent.agent.AgentProperties;
import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderAgentService;
import com.enterprise.agent.agent.OrderTools;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Week 5 Day 1 的大模型例子：先不加固权限，真实演示当前 Agent 既能读订单、也能改订单。
 * 这正好说明为什么 Day 1 要先梳理权限矩阵。
 *
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test ToolPermissionLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class ToolPermissionLiveTest {

    @Test
    void modelCanCurrentlyCallReadAndMutatingToolsWithoutPermissionCheck() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        MockOrderData data = new MockOrderData();
        OrderAgentService orderAgent = new OrderAgentService(
                model, new OrderTools(data), new AgentProperties(3));

        // 1) 只读工具：查订单
        String readReply = orderAgent.chat("查询订单 O1001 的信息");
        System.out.println("[权限矩阵-只读] " + readReply);
        assertThat(readReply).contains("O1001").contains("399");

        // 2) 高危工具：改订单状态
        String writeReply = orderAgent.chat("把订单 O1003 的状态改为 SHIPPED");
        System.out.println("[权限矩阵-改状态] " + writeReply);

        // 当前没有任何权限校验，所以数据真的被改掉了。
        // Day 3 加入 Tool Permission 校验后，这里应该被拒绝。
        assertThat(data.findOrderById("O1003").orElseThrow().status()).isEqualTo("SHIPPED");
    }
}
