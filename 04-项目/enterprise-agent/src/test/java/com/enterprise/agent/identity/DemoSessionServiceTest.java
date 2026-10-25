package com.enterprise.agent.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DemoSessionServiceTest {

    @Test
    @DisplayName("目的：登录身份来自后端账号目录，会话过期或退出后不能继续解析")
    void serverCatalogIssuesAndRevokesTrustedSession() {
        var clock = new MutableClock(Instant.parse("2026-10-25T01:00:00Z"));
        var service = new DemoSessionService(
                new DemoSessionProperties(Duration.ofHours(8)), clock);

        DemoLoginResponse login = service.login("service-a");

        assertThat(login.accessToken()).hasSizeGreaterThan(32);
        assertThat(login.principal().userId()).isEqualTo("user-service-a");
        assertThat(login.principal().tenantId()).isEqualTo("tenant-a");
        assertThat(login.principal().roles()).containsExactly(DemoRole.CUSTOMER_SERVICE);
        assertThat(service.resolve(login.accessToken())).contains(login.principal());

        service.logout(login.accessToken());
        assertThat(service.resolve(login.accessToken())).isEmpty();

        DemoLoginResponse expiring = service.login("manager-a");
        clock.advance(Duration.ofHours(8));
        assertThat(service.resolve(expiring.accessToken())).isEmpty();
    }

    @Test
    @DisplayName("目的：演示目录包含跨租户账号，但登录请求不能自定义租户或角色")
    void accountCatalogKeepsTenantAndRoleOnServer() {
        var service = new DemoSessionService(
                new DemoSessionProperties(Duration.ofHours(8)), Clock.systemUTC());

        assertThat(service.listAccounts())
                .extracting(DemoAccountSummary::accountId)
                .containsExactly(
                        "service-a", "manager-a", "knowledge-a",
                        "auditor-a", "service-b", "platform-admin");
        assertThat(service.login("service-b").principal().tenantId()).isEqualTo("tenant-b");
        assertThatThrownByLogin(service);
    }

    private void assertThatThrownByLogin(DemoSessionService service) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.login("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
