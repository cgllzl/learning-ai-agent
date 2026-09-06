package com.enterprise.agent.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewDecisionTest {

    @Test
    void parsesPassAndRevisionFeedback() {
        assertThat(ReviewDecision.fromModelOutput("PASS").passed()).isTrue();

        ReviewDecision revise = ReviewDecision.fromModelOutput("REVISE:缺少订单金额");
        assertThat(revise.passed()).isFalse();
        assertThat(revise.feedback()).isEqualTo("缺少订单金额");
    }

    @Test
    void malformedReviewOutputCannotSilentlyPass() {
        ReviewDecision decision = ReviewDecision.fromModelOutput("看起来不错");

        assertThat(decision.passed()).isFalse();
        assertThat(decision.feedback()).contains("不符合协议");
    }
}
