package com.enterprise.agent.security.mcp;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;
import com.enterprise.agent.security.ToolRiskLevel;
import com.enterprise.agent.security.agentic.McpToolDescriptor;
import com.enterprise.agent.security.agentic.McpToolTrustVerifier;
import com.enterprise.agent.security.agentic.TrustedMcpToolManifest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * 第三方 MCP Tool 的“门卫”：只有登记过的 Server、未变化的 Tool、足够 Scope，
 * 以及必要的本次展示许可同时满足时，工具才会对大模型可见。
 */
public class McpToolTrustPolicy {

    private final McpServerRegistration server;
    private final McpClient trustedClient;
    private final Map<String, ReviewedMcpTool> reviewedTools;
    private final McpToolTrustVerifier identityVerifier;
    private final AuditLogService auditLog;

    public McpToolTrustPolicy(McpServerRegistration server,
                              McpClient trustedClient,
                              List<ReviewedMcpTool> reviewedTools,
                              McpToolTrustVerifier identityVerifier,
                              AuditLogService auditLog) {
        this.server = server;
        this.trustedClient = Objects.requireNonNull(trustedClient, "trustedClient");
        if (!server.clientKey().equals(trustedClient.key())) {
            throw new IllegalArgumentException("受信 McpClient 的 key 与部署登记不一致");
        }
        Map<String, ReviewedMcpTool> indexedTools = reviewedTools.stream().collect(Collectors.toMap(
                ReviewedMcpTool::toolName,
                tool -> tool,
                (left, right) -> {
                    throw new IllegalArgumentException("审核清单存在重复工具：" + left.toolName());
                },
                LinkedHashMap::new));
        this.reviewedTools = Map.copyOf(indexedTools);
        this.identityVerifier = identityVerifier;
        this.auditLog = auditLog;
    }

    /**
     * 生成可直接交给 {@code McpToolProvider.builder().filter(...)} 的 {@link BiPredicate}。
     * BiPredicate 的第一个参数是工具来自哪个 MCP Client，第二个参数是工具说明；返回 false 后，
     * LangChain4j 不会把该工具的 Schema 或执行器交给模型，所以它是可见性硬门禁，不是事后告警。
     * 返回值捕获了当前 subject/access，必须和本次请求的 ToolProvider 一起使用，不能做跨用户单例。
     */
    public BiPredicate<McpClient, ToolSpecification> filterFor(SecuritySubject subject,
                                                                McpToolAccessContext access) {
        return (client, specification) -> isAllowed(subject, access, client, specification);
    }

    public McpToolChangeSet inspectCatalog(McpClient client) {
        requireTrustedClient(client);
        return inspectCatalog(client.listTools());
    }

    McpToolChangeSet inspectCatalog(List<ToolSpecification> discoveredTools) {
        Map<String, ToolSpecification> discovered = discoveredTools.stream()
                .collect(Collectors.toMap(ToolSpecification::name, tool -> tool));
        Set<String> added = new HashSet<>(discovered.keySet());
        added.removeAll(reviewedTools.keySet());
        Set<String> removed = new HashSet<>(reviewedTools.keySet());
        removed.removeAll(discovered.keySet());
        Set<String> changed = new HashSet<>();
        Set<String> unchanged = new HashSet<>();

        reviewedTools.forEach((name, reviewed) -> {
            ToolSpecification actual = discovered.get(name);
            if (actual == null) {
                return;
            }
            if (reviewed.specificationFingerprint().equals(specificationFingerprint(actual))) {
                unchanged.add(name);
            } else {
                changed.add(name);
            }
        });
        return new McpToolChangeSet(added, removed, changed, unchanged);
    }

    public static String specificationFingerprint(ToolSpecification specification) {
        // toJson() 覆盖名称、描述、完整参数 Schema、metadata 和 strict。
        // 这里故意“宁可误拦也不漏放”：序列化结果有任何变化都要求人工复核。
        return McpToolTrustVerifier.fingerprint(specification.toJson());
    }

    private boolean isAllowed(SecuritySubject subject,
                              McpToolAccessContext access,
                              McpClient client,
                              ToolSpecification actual) {
        if (client != trustedClient || !server.clientKey().equals(client.key())) {
            return block(subject, actual.name(), "MCP Client 实例未与审核部署记录绑定", "unknown-client");
        }
        ReviewedMcpTool reviewed = reviewedTools.get(actual.name());
        if (reviewed == null) {
            return block(subject, actual.name(), "发现未经审核的新工具", specificationFingerprint(actual));
        }
        String actualFingerprint = specificationFingerprint(actual);
        if (!reviewed.specificationFingerprint().equals(actualFingerprint)) {
            return block(subject, actual.name(), "工具描述或参数 Schema 已变化", actualFingerprint);
        }

        try {
            identityVerifier.verify(
                    subject,
                    new McpToolDescriptor(
                            server.serverId(), actual.name(), server.serverVersion(),
                            actual.description(), server.targetHost()),
                    new TrustedMcpToolManifest(
                            server.serverId(), reviewed.toolName(), server.serverVersion(),
                            reviewed.descriptionFingerprint(), Set.of(server.targetHost())));
        } catch (RuntimeException blocked) {
            return false;
        }

        if (!access.grantedScopes().containsAll(reviewed.requiredScopes())) {
            return block(subject, actual.name(), "当前请求缺少最小 Scope", actualFingerprint);
        }
        boolean writeLike = reviewed.riskLevel() != ToolRiskLevel.READ_ONLY;
        if ((writeLike || reviewed.exposureApprovalRequired())
                && !access.exposureApprovedToolNames().contains(actual.name())) {
            return block(subject, actual.name(), "写操作或敏感读取尚未获准在本次请求中展示", actualFingerprint);
        }
        return true;
    }

    private void requireTrustedClient(McpClient client) {
        if (client != trustedClient || !server.clientKey().equals(client.key())) {
            throw new IllegalArgumentException("只能检查与审核部署记录绑定的 McpClient 实例");
        }
    }

    private boolean block(SecuritySubject subject,
                          String toolName,
                          String reason,
                          String fingerprint) {
        String safeFingerprint = fingerprint.length() <= 12
                ? fingerprint
                : fingerprint.substring(0, 12);
        // 审计只记录稳定身份与 hash 前缀，不记录原始 Tool 描述、参数或 Token。
        String digestPrefix = server.artifactDigest().substring("sha256:".length(), "sha256:".length() + 12);
        auditLog.record(subject, "mcpPolicy:" + toolName,
                server.serverId() + "/" + toolName + "#" + safeFingerprint
                        + " artifact=" + digestPrefix,
                reason, AuditStatus.BLOCKED);
        return false;
    }
}
