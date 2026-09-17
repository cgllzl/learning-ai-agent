package com.enterprise.agent.security.mcp;

import java.util.Set;

/**
 * 本次请求能使用的最小权限与“工具是否可展示给模型”的快照。
 *
 * <p>exposureApprovedToolNames 只批准本次请求展示相应工具，并不是参数级业务审批。
 * 写操作真正执行前，仍必须把用户、租户、具体参数、过期时间和次数绑定到执行期审批。</p>
 */
public record McpToolAccessContext(
        Set<String> grantedScopes,
        Set<String> exposureApprovedToolNames
) {
    public McpToolAccessContext {
        grantedScopes = Set.copyOf(grantedScopes);
        exposureApprovedToolNames = Set.copyOf(exposureApprovedToolNames);
    }
}
