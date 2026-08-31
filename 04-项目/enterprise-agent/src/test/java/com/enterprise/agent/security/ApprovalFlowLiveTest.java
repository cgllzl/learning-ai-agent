package com.enterprise.agent.security;

import com.enterprise.agent.agent.MockOrderData;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Week 5 Day 6 的大模型例子：高危操作先进入审批，人工审批后才真正执行，并全程留审计。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test ApprovalFlowLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class ApprovalFlowLiveTest {

    @Test
    void highRiskOperationRequiresHumanApprovalAndIsAudited() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        MockOrderData data = new MockOrderData();
        AuditLogService auditLog = new AuditLogService();
        HumanApprovalService approvals = new HumanApprovalService();
        ApprovalAwareOrderTools tools = new ApprovalAwareOrderTools(data, auditLog, approvals);
        ApprovalAwareOrderAgentService agent = new ApprovalAwareOrderAgentService(model, tools);

        SecuritySubject admin = new SecuritySubject("u1", "t1", Set.of("CUSTOMER_SERVICE", "ORDER_ADMIN"));

        // 1) 第一次请求：生成审批单，不真正改数据
        String firstReply = agent.chat(admin, "把订单 O1003 的状态改为 SHIPPED");
        System.out.println("[审批前回答] " + firstReply);
        assertThat(data.findOrderById("O1003").orElseThrow().status()).isEqualTo("PENDING");
        assertThat(approvals.pendingCount()).isEqualTo(1);

        // 2) 人工审批
        approvals.approve("O1003", "SHIPPED");

        // 3) 再次请求：审批已通过，真正执行
        String secondReply = agent.chat(admin, "请再次把订单 O1003 的状态改为 SHIPPED");
        System.out.println("[审批后回答] " + secondReply);
        assertThat(data.findOrderById("O1003").orElseThrow().status()).isEqualTo("SHIPPED");

        // 4) 审计日志记录了 pending 与 success 两个阶段
        assertThat(auditLog.entries())
                .extracting(AuditLogEntry::status)
                .contains(AuditStatus.PENDING_APPROVAL, AuditStatus.SUCCESS);
    }
}
