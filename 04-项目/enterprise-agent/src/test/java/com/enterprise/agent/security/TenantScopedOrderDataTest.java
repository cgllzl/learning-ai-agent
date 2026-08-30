package com.enterprise.agent.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantScopedOrderDataTest {

    private final TenantScopedOrderData data = new TenantScopedOrderData();

    @Test
    void sameOrderIdIsIsolatedByTenant() {
        assertThat(data.find("t1", "O1001")).hasValueSatisfying(
                text -> assertThat(text).contains("399.0"));
        assertThat(data.find("t2", "O1001")).hasValueSatisfying(
                text -> assertThat(text).contains("1299.0"));
    }

    @Test
    void tenantCannotSeeAnotherTenantsOrder() {
        assertThat(data.find("t1", "O2001")).isEmpty();
        assertThat(data.find("t2", "O2001")).isPresent();
    }
}
