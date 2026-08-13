package com.enterprise.agent.chat;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 DeepSeek 流式联调测试。
 * 默认跳过；本地联调时设置环境变量 DEEPSEEK_API_KEY 后运行：
 *   mvn test -Dtest=StreamingChatServiceLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class StreamingChatServiceLiveTest {

    @Test
    void streamsRealDeepSeekReplyInMultipleChunks() throws Exception {
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();
        StreamingChatService service = new StreamingChatService(model);

        List<String> partials = new ArrayList<>();
        StringBuilder full = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        service.stream(
                "你是企业助手，回答要简洁。",
                List.of(new ChatRequest.Message("user", "用一句话介绍你自己")),
                partial -> {
                    partials.add(partial);
                    full.append(partial);
                },
                latch::countDown,
                e -> {
                    error.set(e);
                    latch.countDown();
                });

        assertThat(latch.await(60, TimeUnit.SECONDS)).as("60 秒内未完成流式响应").isTrue();
        assertThat(error.get()).as("流式调用出错: %s", error.get()).isNull();
        assertThat(partials).as("应收到多个流式分块").hasSizeGreaterThan(1);
        assertThat(full.toString()).isNotBlank();
    }
}