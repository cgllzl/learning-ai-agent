package com.enterprise.agent.security;

/**
 * 密钥包装对象：真实值只通过 raw() 取用，toString() 永远返回脱敏值。
 * 这样即使不小心把对象打进日志，也不会泄露明文。
 */
public final class SecretValue {

    private final String raw;

    private SecretValue(String raw) {
        this.raw = raw;
    }

    public static SecretValue of(String raw) {
        return new SecretValue(raw);
    }

    public String raw() {
        return raw;
    }

    public String masked() {
        return new SecretMasker().mask(raw);
    }

    @Override
    public String toString() {
        return masked();
    }
}
