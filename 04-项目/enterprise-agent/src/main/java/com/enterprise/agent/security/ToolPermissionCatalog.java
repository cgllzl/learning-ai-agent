package com.enterprise.agent.security;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 项目现有 Tool 的权限矩阵（Week 5 Day 1 梳理产物）。
 *
 * 先只做「静态清单」，把每个工具的归属、风险、角色、是否需要人工审批列清楚；
 * Day 2 再接入 RBAC，Day 3 再在 Tool 调用入口做校验。
 */
public final class ToolPermissionCatalog {

    private ToolPermissionCatalog() {
    }

    private static final List<ToolPermission> PERMISSIONS = List.of(
            new ToolPermission("getOrder", "OrderTools",
                    ToolRiskLevel.SENSITIVE_READ, Set.of("CUSTOMER_SERVICE", "SUPERVISOR"), false),
            new ToolPermission("getUser", "OrderTools",
                    ToolRiskLevel.SENSITIVE_READ, Set.of("CUSTOMER_SERVICE", "SUPERVISOR"), false),
            new ToolPermission("getProduct", "OrderTools",
                    ToolRiskLevel.READ_ONLY, Set.of("CUSTOMER_SERVICE", "SUPERVISOR"), false),
            new ToolPermission("getLogistics", "OrderTools",
                    ToolRiskLevel.READ_ONLY, Set.of("CUSTOMER_SERVICE", "SUPERVISOR"), false),
            new ToolPermission("updateOrderStatus", "OrderTools",
                    ToolRiskLevel.MUTATING, Set.of("ORDER_ADMIN", "SUPERVISOR"), true),
            new ToolPermission("handleOrder", "SupervisorTools",
                    ToolRiskLevel.MUTATING, Set.of("SUPERVISOR"), true),
            new ToolPermission("handleKnowledge", "SupervisorTools",
                    ToolRiskLevel.SENSITIVE_READ, Set.of("EMPLOYEE", "SUPERVISOR"), false),
            new ToolPermission("getOrder", "OrderMcpServer",
                    ToolRiskLevel.SENSITIVE_READ, Set.of("EXTERNAL_AGENT"), false));

    public static List<ToolPermission> all() {
        return PERMISSIONS;
    }

    public static Optional<ToolPermission> find(String toolName) {
        return PERMISSIONS.stream()
                .filter(permission -> permission.toolName().equals(toolName))
                .findFirst();
    }

    public static List<ToolPermission> mutatingTools() {
        return PERMISSIONS.stream()
                .filter(permission -> permission.riskLevel() == ToolRiskLevel.MUTATING)
                .toList();
    }

    public static List<ToolPermission> toolsRequiringApproval() {
        return PERMISSIONS.stream()
                .filter(ToolPermission::requiresHumanApproval)
                .toList();
    }
}
