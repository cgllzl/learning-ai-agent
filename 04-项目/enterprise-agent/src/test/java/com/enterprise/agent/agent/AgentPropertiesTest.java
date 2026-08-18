package com.enterprise.agent.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPropertiesTest {

    @Test
    void defaultsToThreeWhenMissing() {
        assertThat(new AgentProperties(null).maxSequentialToolsInvocations()).isEqualTo(3);
    }

    @Test
    void defaultsToThreeWhenTooSmall() {
        assertThat(new AgentProperties(0).maxSequentialToolsInvocations()).isEqualTo(3);
        assertThat(new AgentProperties(-1).maxSequentialToolsInvocations()).isEqualTo(3);
    }

    @Test
    void keepsConfiguredValue() {
        assertThat(new AgentProperties(5).maxSequentialToolsInvocations()).isEqualTo(5);
    }
}