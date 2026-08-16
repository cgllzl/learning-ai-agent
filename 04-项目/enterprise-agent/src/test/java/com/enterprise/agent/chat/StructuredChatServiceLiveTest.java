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
        StructuredChatService service = newService();

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

    @Test
    void classifiesContentFromRealDeepSeek() throws Exception {
        StructuredChatService service = newService();

        JsonNode result = service.structured(
                "你是内容分类助手，对用户输入的内容分类打标。",
                List.of(new ChatRequest.Message("user",
                        "今天发布了一个新的 Java 21 特性教程，讲虚拟线程的用法和性能对比。")),
                "classify",
                null);

        assertThat(result.get("category").asText()).isIn("news", "tech", "life", "other");
        assertThat(result.get("tags").isArray()).isTrue();
        assertThat(result.get("confidence").isNumber()).isTrue();
    }

    @Test
    void parsesResumeFromRealDeepSeek() throws Exception {
        StructuredChatService service = newService();

        JsonNode result = service.structured(
                "你是简历解析助手，从简历文本中抽取结构化信息。",
                List.of(new ChatRequest.Message("user",
                        "我叫李四，有 3 年 Java 后端经验，熟悉 Spring Boot、Redis 和 MySQL，本科学历。")),
                "resume",
                null);

        assertThat(result.get("name").asText()).isNotBlank();
        assertThat(result.get("skills").isArray()).isTrue();
        assertThat(result.get("skills").size()).isGreaterThan(0);
    }

    private StructuredChatService newService() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();
        return new StructuredChatService(model, new ObjectMapper());
    }
}