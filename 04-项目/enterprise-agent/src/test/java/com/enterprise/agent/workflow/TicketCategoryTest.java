package com.enterprise.agent.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketCategoryTest {

    @Test
    void acceptsOnlyOneExactAllowedLabel() {
        assertThat(TicketCategory.fromModelOutput("ORDER")).isEqualTo(TicketCategory.ORDER);
        assertThat(TicketCategory.fromModelOutput(" knowledge ")).isEqualTo(TicketCategory.KNOWLEDGE);
        assertThat(TicketCategory.fromModelOutput("HUMAN")).isEqualTo(TicketCategory.HUMAN);
    }

    @Test
    void failsClosedForAmbiguousOrUnexpectedModelOutput() {
        assertThat(TicketCategory.fromModelOutput("ORDER or KNOWLEDGE"))
                .isEqualTo(TicketCategory.HUMAN);
        assertThat(TicketCategory.fromModelOutput("订单问题，因为……"))
                .isEqualTo(TicketCategory.HUMAN);
        assertThat(TicketCategory.fromModelOutput(null)).isEqualTo(TicketCategory.HUMAN);
    }
}
