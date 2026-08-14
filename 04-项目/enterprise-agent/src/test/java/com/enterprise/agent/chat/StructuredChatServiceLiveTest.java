package com.enterprise.agent.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 DeepSeek 结构化输出联调测试（json_object 兼容模式）。
 * 默认跳过；本地联调时设置环境变量 DEEPSEEK_API_KEY 后运行：
 *   mvn test -Dtest=StructuredChatServiceLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class StructuredChatServiceLiveTest {

    @Test
    void extractsStructuredInfoFromRealDeepSeek() throws Exception {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();
        StructuredChatService service = new StructuredChatService(model, new ObjectMapper());

        JsonNode result = service.structured(
                "你是信息抽取助手，从用户输入中抽取信息。",
                List.of(new ChatRequest.Message("user",
                        "这个产品很好用，就是价格有点贵，不过客服响应很快，整体很满意。")),
                "extract",
                null);

        assertThat(result.get("summary").asText()).isNotBlank();
        assertThat(result.get("sentiment").asText()).isIn("positive", "neutral", "negative");
        assertThat(result.get("keywords").isArray()).isTrue();
        assertThat(result.get("keywords").size()).isGreaterThan(0);
    }
}