package com.enterprise.agent.mcp;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.SecuritySubject;
import com.enterprise.agent.security.ToolRiskLevel;
import com.enterprise.agent.security.agentic.AgenticContentGuard;
import com.enterprise.agent.security.agentic.McpToolTrustVerifier;
import com.enterprise.agent.security.mcp.FailClosedMcpToolErrorHandler;
import com.enterprise.agent.security.mcp.McpServerRegistration;
import com.enterprise.agent.security.mcp.McpToolAccessContext;
import com.enterprise.agent.security.mcp.McpToolChangeSet;
import com.enterprise.agent.security.mcp.McpToolOutputGuard;
import com.enterprise.agent.security.mcp.McpToolTrustPolicy;
import com.enterprise.agent.security.mcp.ReviewedMcpTool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Day 5 第二个重点测试：真实 DeepSeek 只能看到安全策略审核过的只读 MCP Tool。
 *
 * <p>当前没有配置可信第三方远程 Server 与 OAuth 凭证，而且项目依赖尚未支持 MCP 2026-07-28
 * 端到端能力，所以这里用独立本地 stdio Server 模拟第三方服务。大模型调用与 MCP JSON-RPC 链路都是真实的，
 * 但这不是“远程 OAuth 已联调”的证明。</p>
 *
 * <p>运行：{@code .\scripts\test-live.ps1 -Test ThirdPartyMcpLiveTest}</p>
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class ThirdPartyMcpLiveTest {

    private static final SecuritySubject SUBJECT = new SecuritySubject(
            "day5-user", "tenant-a", Set.of("CUSTOMER_SERVICE"));

    @Test
    @DisplayName("目的：真实验证 DeepSeek 通过安全 Provider 调用本地等价第三方 MCP 的只读 Tool")
    void deepSeekCanUseOnlyReviewedReadOnlyToolFromLocalEquivalentServer() throws Exception {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .build();

        McpClient mcpClient = startLocalEquivalentThirdPartyServer();
        try {
            AuditLogService audit = new AuditLogService();
            McpToolTrustPolicy policy = reviewedReadOnlyPolicy(mcpClient, audit);
            McpToolChangeSet changes = policy.inspectCatalog(mcpClient);
            assertThat(changes.requiresReview()).isFalse();
            assertThat(changes.unchanged()).containsExactly("getOrder");

            /*
             * filter 接收 BiPredicate<McpClient, ToolSpecification>：
             * - client 告诉策略“工具来自哪个 Server”；
             * - specification 提供名称、描述和完整参数 Schema；
             * - 返回 false 时，该工具不会进入模型看到的 Tool 列表，也不会生成执行器。
             */
            McpToolProvider secureToolProvider = McpToolProvider.builder()
                    .mcpClients(mcpClient)
                    .filter(policy.filterFor(
                            SUBJECT,
                            new McpToolAccessContext(Set.of("orders:read"), Set.of())))
                    // Tool 描述没变也不代表返回值安全；运行结果在送回模型前再做一次不可信内容检查。
                    .toolWrapper(new McpToolOutputGuard(
                            new AgenticContentGuard(), audit).wrapperFor(SUBJECT))
                    .failIfOneServerFails(true)
                    .build();

            // AiServices 把接口方法变成模型调用；toolProvider 只接收安全门禁筛选后的 MCP Tool。
            McpOrderAssistant assistant = AiServices.builder(McpOrderAssistant.class)
                    .chatModel(model)
                    .toolProvider(secureToolProvider)
                    // 默认错误处理可能把异常文本交回模型继续推理；安全拦截必须 fail closed。
                    .toolExecutionErrorHandler(new FailClosedMcpToolErrorHandler())
                    .build();

            String answer = assistant.chat("查询订单 O1001 的金额和状态");
            System.out.println("[Day5 安全 MCP 联调] " + answer);

            assertThat(answer).contains("O1001", "399");
        } finally {
            mcpClient.close();
        }
    }

    private McpToolTrustPolicy reviewedReadOnlyPolicy(McpClient trustedClient,
                                                       AuditLogService audit) {
        ToolSpecification reviewedSpecification = ToolSpecification.builder()
                .name("getOrder")
                .description("根据订单号查询订单信息")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("orderId", "订单号，例如 O1001")
                        .required("orderId")
                        .build())
                .build();
        return new McpToolTrustPolicy(
                new McpServerRegistration(
                        "third-party-order-stdio",
                        "local-equivalent-third-party-order",
                        "1.0.0",
                        "sha256:" + "b".repeat(64),
                        "local-stdio-process",
                        URI.create("https://local-equivalent.invalid/mcp")),
                trustedClient,
                List.of(ReviewedMcpTool.from(
                        reviewedSpecification,
                        ToolRiskLevel.READ_ONLY,
                        Set.of("orders:read"),
                        false)),
                new McpToolTrustVerifier(audit),
                audit);
    }

    private McpClient startLocalEquivalentThirdPartyServer() {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        List<String> command = List.of(
                javaBin,
                "-cp",
                resolveClasspath(),
                OrderMcpServer.class.getName());

        /*
         * protocolVersion 固定为当前双方都支持的 2025-11-25，避免误称已经使用 2026 新线。
         * cacheToolList(false) 让每次 Agent 调用重新读取工具清单；安全策略随后逐项比对指纹，
         * 因而不依赖第三方 Server 是否诚实发送 tools/list_changed 通知。
         */
        return DefaultMcpClient.builder()
                .key("third-party-order-stdio")
                .transport(StdioMcpTransport.builder().command(command).build())
                .protocolVersion("2025-11-25")
                .cacheToolList(false)
                .build();
    }

    private String resolveClasspath() {
        String classpath = System.getProperty("surefire.test.class.path");
        if (classpath == null || classpath.isBlank()) {
            classpath = System.getProperty("java.class.path");
        }
        if (classpath == null || classpath.isBlank()) {
            classpath = "target/classes;target/test-classes";
        }
        return classpath;
    }
}
