package com.enterprise.agent.identity;

import com.enterprise.agent.workflow.TicketCategory;
import com.enterprise.agent.workflow.WorkbenchRouteRequest;
import com.enterprise.agent.workflow.WorkbenchRouteResult;
import com.enterprise.agent.workflow.WorkbenchView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 运行：.\scripts\test-live.ps1 -Test DemoAuthenticatedWorkbenchLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoAuthenticatedWorkbenchLiveTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("目的：真实HTTP登录后携带后端会话调用DeepSeek路由，前端不提交租户或角色")
    void loginThenRouteOrderQuestionThroughRealModel() {
        String baseUrl = "http://127.0.0.1:" + port;
        DemoLoginResponse login = restTemplate.postForObject(
                baseUrl + "/api/v1/auth/demo-login",
                new DemoLoginRequest("service-a"),
                DemoLoginResponse.class);
        assertThat(login).isNotNull();
        assertThat(login.principal().tenantId()).isEqualTo("tenant-a");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.accessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WorkbenchRouteRequest> request = new HttpEntity<>(
                new WorkbenchRouteRequest("查询订单 O1001 的金额和状态"), headers);

        WorkbenchRouteResult result = restTemplate.postForObject(
                baseUrl + "/api/v1/workbench/route",
                request,
                WorkbenchRouteResult.class);

        System.out.printf("[Phase2 P1D2 Auth Route] principal=%s, result=%s%n",
                login.principal(), result);
        assertThat(result).isNotNull();
        assertThat(result.category()).isEqualTo(TicketCategory.ORDER);
        assertThat(result.suggestedView()).isEqualTo(WorkbenchView.ORDERS);
        assertThat(result.executionStatus()).isEqualTo("NOT_EXECUTED");
    }
}
