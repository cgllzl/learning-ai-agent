package com.enterprise.agent.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentWorkflowPropertiesTest {

    @Test
    void usesSafeDefaultWhenValueIsMissingOrInvalid() {
        assertThat(new AgentWorkflowProperties(null).maxReviewIterations()).isEqualTo(3);
        assertThat(new AgentWorkflowProperties(0).maxReviewIterations()).isEqualTo(3);
    }

    @Test
    void acceptsConfiguredLimit() {
        assertThat(new AgentWorkflowProperties(2).maxReviewIterations()).isEqualTo(2);
    }
}
