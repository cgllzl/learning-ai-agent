package com.enterprise.agent.agent;

import com.enterprise.agent.chat.DeepSeekProperties;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 DeepSeek Tool Calling 联调测试（Day 2/4/5/6）。
 * 默认跳过；本地联调时设置环境变量 DEEPSEEK_API_KEY 后运行：
 *   .\scripts\test-live.ps1 -Test OrderAgentLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class OrderAgentLiveTest {

    /** 测试用：故意抛异常的工具，验证 Agent 对工具报错的兜底。 */
    public static class FlakyTools {
        @Tool("无论调用什么都会抛异常的测试工具")
        public String alwaysFail(@P("任意入参") String anything) {
            throw new IllegalStateException("模拟工具崩溃");
        }
    }

    @Test
    void agentCallsOrderToolForOrderQuery() {
        OrderAgentService service = newService();
        String reply = service.chat("查询订单 O1001 的信息");
        System.out.println("[查订单回复] " + reply);
        assertThat(reply).contains("O1001");
        assertThat(reply).contains("399");
    }

    @Test
    void agentCallsUserTool() {
        OrderAgentService service = newService();
        String reply = service.chat("查一下用户 U1 的信息");
        System.out.println("[查用户回复] " + reply);
        assertThat(reply).contains("张三");
    }

    @Test
    void agentCallsLogisticsTool() {
        OrderAgentService service = newService();
        String reply = service.chat("帮我查一下订单 O1002 的物流信息");
        System.out.println("[查物流回复] " + reply);
        assertThat(reply).contains("顺丰");
    }

    @Test
    void agentUpdatesPendingOrderStatus() {
        OrderAgentService service = newService();
        String reply = service.chat("把订单 O1003 的状态改为 SHIPPED");
        System.out.println("[改状态回复] " + reply);
        assertThat(reply).contains("O1003");
        assertThat(reply).contains("SHIPPED");
    }

    @Test
    void agentSurvivesToolFailure() {
        DeepSeekProperties props = new DeepSeekProperties(
                System.getenv("DEEPSEEK_API_KEY"), "https://api.deepseek.com", "deepseek-chat",
                Duration.ofSeconds(60), 2, "deepseek-chat");
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl(props.baseUrl()).apiKey(props.apiKey())
                .modelName(props.model()).timeout(props.timeout()).build();

        // 注册一个会抛异常的工具 + 防循环上限 3
        OrderAssistant assistant = AiServices.builder(OrderAssistant.class)
                .chatModel(model)
                .tools(new OrderTools(new MockOrderData()), new FlakyTools())
                .maxSequentialToolsInvocations(3)
                .build();

        String reply = assistant.chat("调用 alwaysFail 工具看看会发生什么");
        System.out.println("[工具报错兜底回复] " + reply);
        // 工具抛异常后 Agent 不应崩溃，应给出"失败/无法/抱歉"之类的兜底回答
        assertThat(reply).isNotBlank();
        assertThat(reply).containsAnyOf("失败", "无法", "异常", "出错", "抱歉", "问题");
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
        return new OrderAgentService(model, new OrderTools(new MockOrderData()), new AgentProperties(3));
    }
}