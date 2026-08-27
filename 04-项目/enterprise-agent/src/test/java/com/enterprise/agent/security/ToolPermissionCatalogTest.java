package com.enterprise.agent.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolPermissionCatalogTest {

    @Test
    void catalogContainsAllExistingTools() {
        assertThat(ToolPermissionCatalog.all())
                .extracting(ToolPermission::toolName)
                .contains(
                        "getOrder",
                        "getUser",
                        "getProduct",
                        "getLogistics",
                        "updateOrderStatus",
                        "handleOrder",
                        "handleKnowledge");
    }

    @Test
    void updateOrderStatusIsMutatingAndRequiresApproval() {
        ToolPermission updateOrderStatus = ToolPermissionCatalog.find("updateOrderStatus").orElseThrow();

        assertThat(updateOrderStatus.riskLevel()).isEqualTo(ToolRiskLevel.MUTATING);
        assertThat(updateOrderStatus.requiresHumanApproval()).isTrue();
    }

    @Test
    void getOrderIsReadOnlyWithoutApproval() {
        ToolPermission getOrder = ToolPermissionCatalog.find("getOrder").orElseThrow();

        assertThat(getOrder.riskLevel()).isEqualTo(ToolRiskLevel.SENSITIVE_READ);
        assertThat(getOrder.requiresHumanApproval()).isFalse();
    }

    @Test
    void catalogIdentifiesAllMutatingTools() {
        assertThat(ToolPermissionCatalog.mutatingTools())
                .extracting(ToolPermission::toolName)
                .contains("updateOrderStatus", "handleOrder");
    }
}
