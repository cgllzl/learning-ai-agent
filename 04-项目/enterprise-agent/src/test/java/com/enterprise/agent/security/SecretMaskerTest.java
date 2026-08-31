package com.enterprise.agent.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretMaskerTest {

    private final SecretMasker masker = new SecretMasker();

    @Test
    void longSecretKeepsOnlyFirstAndLastFourCharacters() {
        String masked = masker.mask("sk-1234567890abcdefghij");

        assertThat(masked).isEqualTo("sk-1****ghij");
        assertThat(masked).doesNotContain("1234567890abcdefghij");
    }

    @Test
    void shortSecretIsFullyMasked() {
        assertThat(masker.mask("abc")).isEqualTo("****");
    }
}
