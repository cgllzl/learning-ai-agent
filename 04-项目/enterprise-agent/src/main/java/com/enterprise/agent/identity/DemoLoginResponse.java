package com.enterprise.agent.identity;

import java.time.Instant;

public record DemoLoginResponse(
        String tokenType,
        String accessToken,
        Instant expiresAt,
        DemoPrincipal principal
) {
}
