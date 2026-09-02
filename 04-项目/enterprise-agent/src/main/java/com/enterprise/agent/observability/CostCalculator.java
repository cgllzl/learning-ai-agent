package com.enterprise.agent.observability;

/**
 * 按输入/输出 Token 单价计算成本。
 * 价格单位：每 100 万 Token 的美元金额。
 */
public class CostCalculator {

    private final double inputPricePerMillion;
    private final double outputPricePerMillion;

    public CostCalculator(double inputPricePerMillion, double outputPricePerMillion) {
        this.inputPricePerMillion = inputPricePerMillion;
        this.outputPricePerMillion = outputPricePerMillion;
    }

    public double calculateUsd(int inputTokens, int outputTokens) {
        double inputCost = inputTokens / 1_000_000.0 * inputPricePerMillion;
        double outputCost = outputTokens / 1_000_000.0 * outputPricePerMillion;
        return inputCost + outputCost;
    }
}
