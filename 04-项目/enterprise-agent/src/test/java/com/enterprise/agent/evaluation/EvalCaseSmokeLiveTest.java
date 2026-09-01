package com.enterprise.agent.evaluation;

import com.enterprise.agent.agent.AgentProperties;
import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderAgentService;
import com.enterprise.agent.agent.OrderTools;
import com.enterprise.agent.security.RbacService;
import com.enterprise.agent.security.SecureOrderAgentService;
import com.enterprise.agent.security.SecuritySubject;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Week 6 Day 1 的大模型例子：从评估用例清单里挑两个核心场景做冒烟验证。
 * 其中 TENANT_ISOLATION 是贴合企业 SaaS 场景的例子。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test EvalCaseSmokeLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class EvalCaseSmokeLiveTest {

    @Test
    void runsTwoRepresentativeCases() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        // 学习场景：订单查询正确性
        AgentEvalCase orderQuery = AgentEvalCaseCatalog.byId("ORDER_QUERY");
        OrderAgentService orderAgent = new OrderAgentService(
                model, new OrderTools(new MockOrderData()), new AgentProperties(3));
        String orderReply = orderAgent.chat(orderQuery.input());
        System.out.println("[评估用例-订单查询] " + orderReply);
        assertThat(orderReply).contains("O1001").contains("399");

        // 企业场景：SaaS 多租户隔离
        AgentEvalCase tenantIsolation = AgentEvalCaseCatalog.byId("TENANT_ISOLATION");
        SecureOrderAgentService secureOrderAgent =
                new SecureOrderAgentService(model, new RbacService());

        SecuritySubject t1Cs = new SecuritySubject("u1", "t1", Set.of("CUSTOMER_SERVICE"));
        String t1Reply = secureOrderAgent.chat(t1Cs, "查询订单 O1001 的信息");
        System.out.println("[企业用例-租户 t1] " + t1Reply);
        assertThat(t1Reply).contains("399");

        SecuritySubject t2Cs = new SecuritySubject("u2", "t2", Set.of("CUSTOMER_SERVICE"));
        String t2Reply = secureOrderAgent.chat(t2Cs, "查询订单 O1001 的信息");
        System.out.println("[企业用例-租户 t2] " + t2Reply);
        assertThat(t2Reply).contains("1299");

        // 确认清单里的两条用例都被冒烟执行了
        assertThat(orderQuery.id()).isEqualTo("ORDER_QUERY");
        assertThat(tenantIsolation.id()).isEqualTo("TENANT_ISOLATION");
    }
}
