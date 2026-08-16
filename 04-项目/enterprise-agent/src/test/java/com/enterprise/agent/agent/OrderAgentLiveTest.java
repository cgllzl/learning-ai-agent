package com.enterprise.agent.agent;

import com.enterprise.agent.chat.DeepSeekProperties;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 DeepSeek Tool Calling 联调测试：验证 Agent 能调用 Java 工具。
 * 默认跳过；本地联调时设置环境变量 DEEPSEEK_API_KEY 后运行：
 *   mvn test -Dtest=OrderAgentLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class OrderAgentLiveTest {

    @Test
    void agentCallsOrderToolForOrderQuery() {
        OrderAgentService service = newService();

        String reply = service.chat("查询订单 O1001 的信息");

        // 商品名/金额只可能来自工具返回，能出现说明 Agent 真的调用了 Java 工具
        assertThat(reply).contains("O1001");
        assertThat(reply).contains("机械键盘");
        assertThat(reply).contains("399");
    }

    @Test
    void agentCallsLogisticsTool() {
        OrderAgentService service = newService();

        String reply = service.chat("帮我查一下订单 O1002 的物流信息");

        assertThat(reply).contains("顺丰");
    }

    @Test
    void agentUpdatesPendingOrderAfterConfirmation() {
        OrderAgentService service = newService();

        String reply = service.chat("把订单 O1003 的状态改为 SHIPPED");

        assertThat(reply).contains("O1003");
        assertThat(reply).contains("SHIPPED");
    }

    private OrderAgentService newService() {
        DeepSeekProperties props = new DeepSeekProperties(
                System.getenv("DEEPSEEK_API_KEY"), "https://api.deepseek.com", "deepseek-chat",
                Duration.ofSeconds(60), 2, "deepseek-chat");
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl(props.baseUrl()).apiKey(props.apiKey())
                .modelName(props.model()).timeout(props.timeout()).build();
        return new OrderAgentService(model, new OrderTools(new MockOrderData()));
    }
}