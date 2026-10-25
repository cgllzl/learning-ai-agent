package com.enterprise.agent.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 学习期的服务端会话：随机令牌只做索引，身份、租户和角色以服务端目录为准。
 */
@Service
public class DemoSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DemoSessionProperties properties;
    private final Clock clock;
    private final Map<String, DemoPrincipal> accounts;
    private final Map<String, StoredSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    public DemoSessionService(DemoSessionProperties properties) {
        this(properties, Clock.systemUTC());
    }

    DemoSessionService(DemoSessionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.accounts = createAccounts();
    }

    public List<DemoAccountSummary> listAccounts() {
        List<DemoAccountSummary> result = new ArrayList<>();
        accounts.forEach((accountId, principal) -> result.add(new DemoAccountSummary(
                accountId,
                principal.displayName(),
                principal.tenantId(),
                principal.tenantName(),
                principal.roles()
        )));
        return List.copyOf(result);
    }

    public DemoLoginResponse login(String accountId) {
        DemoPrincipal principal = accounts.get(accountId);
        if (principal == null) {
            throw new IllegalArgumentException("未知演示账号");
        }

        String token = newToken();
        Instant expiresAt = clock.instant().plus(properties.sessionTtl());
        sessions.put(token, new StoredSession(principal, expiresAt));
        return new DemoLoginResponse("Bearer", token, expiresAt, principal);
    }

    public Optional<DemoPrincipal> resolve(String token) {
        StoredSession session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (!session.expiresAt().isAfter(clock.instant())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session.principal());
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Map<String, DemoPrincipal> createAccounts() {
        Map<String, DemoPrincipal> catalog = new LinkedHashMap<>();
        add(catalog, "service-a", "客服小林", "tenant-a", "华东零售", DemoRole.CUSTOMER_SERVICE);
        add(catalog, "manager-a", "运营主管周敏", "tenant-a", "华东零售", DemoRole.OPERATIONS_MANAGER);
        add(catalog, "knowledge-a", "知识管理员陈晨", "tenant-a", "华东零售", DemoRole.KNOWLEDGE_ADMIN);
        add(catalog, "auditor-a", "审计员赵宁", "tenant-a", "华东零售", DemoRole.AUDITOR);
        add(catalog, "service-b", "客服王洁", "tenant-b", "华南供应链", DemoRole.CUSTOMER_SERVICE);
        add(catalog, "platform-admin", "平台管理员", "platform", "平台运营", DemoRole.SYSTEM_ADMIN);
        // 保留教学与演示顺序：先展示业务角色，再展示平台管理员。
        return Collections.unmodifiableMap(catalog);
    }

    private void add(Map<String, DemoPrincipal> catalog,
                     String accountId,
                     String displayName,
                     String tenantId,
                     String tenantName,
                     DemoRole... roles) {
        catalog.put(accountId, new DemoPrincipal(
                "user-" + accountId, displayName, tenantId, tenantName, Set.of(roles)));
    }

    private record StoredSession(DemoPrincipal principal, Instant expiresAt) {
    }
}
