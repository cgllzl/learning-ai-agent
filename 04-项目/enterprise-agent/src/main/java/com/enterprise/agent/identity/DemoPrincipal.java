package com.enterprise.agent.identity;

import java.util.Set;

/**
 * 后端从会话恢复的受信身份；业务代码不能用前端请求体替代它。
 */
public record DemoPrincipal(
        String userId,
        String displayName,
        String tenantId,
        String tenantName,
        Set<DemoRole> roles
) {
    public DemoPrincipal {
        roles = Set.copyOf(roles);
    }
}
