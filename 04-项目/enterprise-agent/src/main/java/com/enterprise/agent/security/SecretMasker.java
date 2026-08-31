package com.enterprise.agent.security;

/**
 * 密钥脱敏工具：只保留头尾各 4 位，中间用 **** 替代。
 */
public class SecretMasker {

    public String mask(String secret) {
        if (secret == null) {
            return null;
        }
        if (secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}
