package com.enterprise.agent.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CostCalculatorTest {

    @Test
    void calculatesCostFromInputAndOutputTokens() {
        CostCalculator calculator = new CostCalculator(1.0, 2.0);

        // 输入 1M token 计 1 美元，输出 0.5M token 计 1 美元
        assertThat(calculator.calculateUsd(1_000_000, 500_000)).isEqualTo(2.0);
    }
}
