package com.enterprise.agent.workflow;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 企业工单 Workflow 的硬限制。
 *
 * 循环次数由代码配置控制，不能由大模型自行决定，避免无限循环和成本失控。
 */
@ConfigurationProperties(prefix = "agent.workflow")
public record AgentWorkflowProperties(Integer maxReviewIterations) {

    public AgentWorkflowProperties {
        if (maxReviewIterations == null || maxReviewIterations < 1) {
            maxReviewIterations = 3;
        }
    }
}
