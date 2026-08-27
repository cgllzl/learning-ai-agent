package com.enterprise.agent.security;

import java.util.Set;

/**
 * 一个 Tool 的权限条目：属于谁、有多危险、谁能用、是否需要人工审批。
 */
public record ToolPermission(
        String toolName,
        String owner,
        ToolRiskLevel riskLevel,
        Set<String> requiredRoles,
        boolean requiresHumanApproval) {
}
