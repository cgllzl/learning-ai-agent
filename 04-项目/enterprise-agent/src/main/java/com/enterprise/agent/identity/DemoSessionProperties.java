package com.enterprise.agent.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 演示会话有效期；未配置或非法值统一回退到8小时。
 */
@ConfigurationProperties(prefix = "demo.auth")
public record DemoSessionProperties(Duration sessionTtl) {

    public DemoSessionProperties {
        if (sessionTtl == null || sessionTtl.isZero() || sessionTtl.isNegative()) {
            sessionTtl = Duration.ofHours(8);
        }
    }
}
