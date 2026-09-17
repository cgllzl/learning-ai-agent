package com.enterprise.agent.security.mcp;

import com.enterprise.agent.security.ToolRiskLevel;
import com.enterprise.agent.security.agentic.McpToolTrustVerifier;
import dev.langchain4j.agent.tool.ToolSpecification;

import java.util.Objects;
import java.util.Set;

/**
 * 安全评审后冻结的单个 MCP Tool 清单项。
 *
 * <p>只允许工具名远远不够：恶意 Server 可以保留同名工具，却偷偷改变描述或参数 Schema。
 * 因此同时固定描述指纹和完整 {@link ToolSpecification} 指纹。</p>
 */
public record ReviewedMcpTool(
        String toolName,
        String descriptionFingerprint,
        String specificationFingerprint,
        ToolRiskLevel riskLevel,
        Set<String> requiredScopes,
        boolean exposureApprovalRequired
) {

    public ReviewedMcpTool {
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(descriptionFingerprint, "descriptionFingerprint");
        Objects.requireNonNull(specificationFingerprint, "specificationFingerprint");
        Objects.requireNonNull(riskLevel, "riskLevel");
        requiredScopes = Set.copyOf(requiredScopes);
    }

    public static ReviewedMcpTool from(ToolSpecification specification,
                                       ToolRiskLevel riskLevel,
                                       Set<String> requiredScopes,
                                       boolean exposureApprovalRequired) {
        return new ReviewedMcpTool(
                specification.name(),
                McpToolTrustVerifier.fingerprint(specification.description()),
                McpToolTrustPolicy.specificationFingerprint(specification),
                riskLevel,
                requiredScopes,
                exposureApprovalRequired);
    }
}
