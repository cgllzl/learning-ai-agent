package com.enterprise.agent.workflow.durable;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DurableWorkflowPlanTest {

    @Test
    void acceptsOnlyWhitelistedPlan() {
        DurableWorkflowPlan plan = DurableWorkflowPlan.fromModelOutput(
                "UPDATE_ORDER,SEND_NOTIFICATION");

        assertThat(plan.requiresHuman()).isFalse();
        assertThat(plan.steps()).containsExactly(
                AgentStep.UPDATE_ORDER, AgentStep.SEND_NOTIFICATION);
    }

    @Test
    void acceptsHarmlessWhitespaceAndCodeFenceWithoutRelaxingStepWhitelist() {
        DurableWorkflowPlan plan = DurableWorkflowPlan.fromModelOutput("""
                ```text
                UPDATE_ORDER, SEND_NOTIFICATION
                ```
                """);

        assertThat(plan.requiresHuman()).isFalse();
        assertThat(plan.steps()).containsExactly(
                AgentStep.UPDATE_ORDER, AgentStep.SEND_NOTIFICATION);
    }

    @Test
    void unexpectedOrExtraStepFailsClosed() {
        assertThat(DurableWorkflowPlan.fromModelOutput("UPDATE_ORDER,DELETE_USER").requiresHuman())
                .isTrue();
        assertThat(DurableWorkflowPlan.fromModelOutput("HUMAN").steps())
                .isEqualTo(List.of());
    }
}
