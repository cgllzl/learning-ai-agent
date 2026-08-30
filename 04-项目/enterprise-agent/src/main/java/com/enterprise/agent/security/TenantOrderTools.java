package com.enterprise.agent.security;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * 租户隔离版订单工具：数据从 TenantContext 当前租户里取。
 */
public class TenantOrderTools {

    private final TenantScopedOrderData data;

    public TenantOrderTools(TenantScopedOrderData data) {
        this.data = data;
    }

    @Tool("根据订单号查询订单信息")
    public String getOrder(@P("订单号，例如 O1001") String orderId) {
        String tenantId = TenantContext.requiredTenantId();
        return data.find(tenantId, orderId)
                .orElse("未找到订单 " + orderId);
    }
}
