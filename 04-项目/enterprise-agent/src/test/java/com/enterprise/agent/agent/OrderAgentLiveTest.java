package com.enterprise.agent.agent;

import com.enterprise.agent.chat.DeepSeekProperties;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 DeepSeek Tool Calling 联调测试（Day 2/4）。
 * 默认跳过；本地联调时设置环境变量 DEEPSEEK_API_KEY 后运行：
 *   .\scripts\test-live.ps1 -Test OrderAgentLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class OrderAgentLiveTest {

    @Test
    void agentCallsOrderToolForOrderQuery() {
        OrderAgentService service = newService();

        String reply = service.chat("查询订单 O1001 的信息");

        System.out.println("[查订单回复] " + reply);
        // 金额只可能来自工具返回值，能出现说明模型真的调用了 Java 工具
        assertThat(reply).contains("O1001");
        assertThat(reply).contains("399");
    }

    @Test
    void agentCallsUserTool() {
        OrderAgentService service = newService();

        String reply = service.chat("查一下用户 U1 的信息");

        System.out.println("[查用户回复] " + reply);
        // 张三只可能来自 getUser 工具的返回值
        assertThat(reply).contains("张三");
    }

    @Test
    void agentCallsLogisticsTool() {
        OrderAgentService service = newService();

        String reply = service.chat("帮我查一下订单 O1002 的物流信息");

        System.out.println("[查物流回复] " + reply);
        // 顺丰只可能来自 getLogistics 工具的返回值
        assertThat(reply).contains("顺丰");
    }

    private OrderAgentService newService() {
        DeepSeekProperties props = new DeepSeekProperties(
                System.getenv("DEEPSEEK_API_KEY"), "https://api.deepseek.com", "deepseek-chat",
                Duration.ofSeconds(60), 2, "deepseek-chat");
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl(props.baseUrl()).apiKey(props.apiKey())
                .modelName(props.model()).timeout(props.timeout())
                .logRequests(true).logResponses(true)
                .build();
        return new OrderAgentService(model, new OrderTools(new MockOrderData()));
    }
}