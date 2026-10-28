package com.enterprise.agent.workbench;

import com.enterprise.agent.identity.DemoLoginRequest;
import com.enterprise.agent.identity.DemoLoginResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** 运行：.\scripts\test-live.ps1 -Test WorkbenchStreamLiveTest */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkbenchStreamLiveTest {

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate restTemplate;

    @Test
    @DisplayName("真实HTTP登录后，DeepSeek增量回答携带当前租户引用、步骤、traceId和完成终态")
    void authenticatedKnowledgeQuestionStreamsRealModelAnswer() throws Exception {
        String base = "http://127.0.0.1:" + port;
        DemoLoginResponse login = restTemplate.postForObject(base + "/api/v1/auth/demo-login",
                new DemoLoginRequest("service-a"), DemoLoginResponse.class);
        assertThat(login).isNotNull();

        // POST SSE 需要 Authorization Header，因此使用 HttpClient 读取事件流直到 done。
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/v1/workbench/query/stream"))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + login.accessToken())
                .header("Accept", "text/event-stream")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"message\":\"华东零售售后申请期限是什么？\",\"mode\":\"KNOWLEDGE\"}",
                        StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type").orElse("")).contains("text/event-stream");
        assertThat(response.body()).contains("event:step", "event:citations", "tenant-a-after-sales",
                "event:delta", "event:done", "\"traceId\"", "\"status\":\"COMPLETED\"");
        assertThat(response.body()).doesNotContain("tenant-b-after-sales");
        System.out.printf("[P1D3 Live SSE] %s%n", response.body());
    }
}
