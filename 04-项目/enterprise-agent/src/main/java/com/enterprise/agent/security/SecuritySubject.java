package com.enterprise.agent.security;

import java.util.Set;

/**
 * 当前请求的「安全主体」：谁、属于哪个租户、有哪些角色。
 * 它是 RBAC 和租户隔离的共同输入。
 */
public record SecuritySubject(String userId, String tenantId, Set<String> roles) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
