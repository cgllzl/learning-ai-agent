package com.enterprise.agent.security;

import java.util.Arrays;
import java.util.Set;

/**
 * 最小化的 RBAC 判断器：检查当前主体是否拥有某个角色。
 */
public class RbacService {

    public boolean hasAnyRole(SecuritySubject subject, String... requiredRoles) {
        Set<String> roles = subject.roles();
        return Arrays.stream(requiredRoles).anyMatch(roles::contains);
    }

    public void checkAnyRole(SecuritySubject subject, String... requiredRoles) {
        if (!hasAnyRole(subject, requiredRoles)) {
            throw new AgentAccessDeniedException(
                    "用户 " + subject.userId() + " 缺少所需角色之一：" + Arrays.toString(requiredRoles));
        }
    }
}
