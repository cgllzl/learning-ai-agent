package com.enterprise.agent.identity;

import java.util.Set;

/**
 * 登录页可见的演示账号信息，不包含访问令牌或服务端会话状态。
 */
public record DemoAccountSummary(
        String accountId,
        String displayName,
        String tenantId,
        String tenantName,
        Set<DemoRole> roles
) {
}
