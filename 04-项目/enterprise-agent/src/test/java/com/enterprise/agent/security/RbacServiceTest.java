package com.enterprise.agent.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RbacServiceTest {

    private final RbacService rbacService = new RbacService();

    @Test
    void hasAnyRoleReturnsTrueWhenSubjectHasOneRequiredRole() {
        SecuritySubject subject = new SecuritySubject("u1", "t1", Set.of("EMPLOYEE", "CUSTOMER_SERVICE"));

        assertThat(rbacService.hasAnyRole(subject, "CUSTOMER_SERVICE", "SUPERVISOR")).isTrue();
    }

    @Test
    void checkAnyRoleThrowsWhenSubjectHasNoRequiredRole() {
        SecuritySubject subject = new SecuritySubject("u1", "t1", Set.of("EMPLOYEE"));

        assertThatThrownBy(() -> rbacService.checkAnyRole(subject, "CUSTOMER_SERVICE", "SUPERVISOR"))
                .isInstanceOf(AgentAccessDeniedException.class)
                .hasMessageContaining("缺少所需角色");
    }
}
