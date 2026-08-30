package com.enterprise.agent.security;

import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.service.tool.ToolService;

import java.util.List;
import java.util.Set;

/**
 * 按当前用户的角色动态过滤 Tool。
 * AiServices 每次需要工具列表时，都会调用 provideTools；这里从 TenantContext 拿到当前用户，
 * 再按 Day 1 的权限矩阵把没有权限的工具过滤掉。
 */
public class PermissionAwareToolProvider implements ToolProvider {

    private final Object toolsObject;

    public PermissionAwareToolProvider(Object toolsObject) {
        this.toolsObject = toolsObject;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        SecuritySubject subject = TenantContext.current();
        List<AiServiceTool> allTools = ToolService.findTools(toolsObject);

        List<AiServiceTool> allowedTools = allTools.stream()
                .filter(tool -> hasPermission(tool.name(), subject.roles()))
                .toList();

        return ToolProviderResult.builder()
                .addAll(allowedTools)
                .build();
    }

    private boolean hasPermission(String toolName, Set<String> roles) {
        return ToolPermissionCatalog.find(toolName)
                .map(permission -> permission.requiredRoles().stream().anyMatch(roles::contains))
                .orElse(false);
    }

    @Override
    public boolean isDynamic() {
        // 每次用户请求都可能不同，必须每次都重新按当前用户过滤
        return true;
    }
}
