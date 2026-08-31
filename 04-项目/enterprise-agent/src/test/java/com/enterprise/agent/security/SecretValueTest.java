package com.enterprise.agent.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretValueTest {

    @Test
    void toStringNeverLeaksRawValue() {
        String raw = "sk-1234567890abcdefghij";
        SecretValue secret = SecretValue.of(raw);

        assertThat(secret.raw()).isEqualTo(raw);
        assertThat(secret.toString()).doesNotContain(raw);
        assertThat(secret.toString()).contains("****");
    }
}
