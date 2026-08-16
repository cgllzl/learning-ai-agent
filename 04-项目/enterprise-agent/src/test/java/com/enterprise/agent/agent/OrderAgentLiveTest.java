package com.enterprise.agent.agent;

import com.enterprise.agent.chat.DeepSeekProperties;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 DeepSeek 联调：验证注册的 Java 工具能被模型调用（Day 2）。
 * 默认跳过；本地联调时设置环境变量 DEEPSEEK_API_KEY 后运行：
 *   mvn test -Dtest=OrderAgentLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class OrderAgentLiveTest {

    @Test
    void agentCallsOrderToolForOrderQuery() {
        OrderAgentService service = newService();

        String reply = service.chat("查询订单 O1001 的信息");

        // 金额/状态只可能来自工具返回值，能出现说明模型真的调用了 Java 工具
        assertThat(reply).contains("O1001");
        assertThat(reply).contains("399");
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