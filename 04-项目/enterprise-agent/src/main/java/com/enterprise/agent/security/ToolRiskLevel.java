package com.enterprise.agent.security;

/**
 * 工具的风险等级（用于 Week 5 Day 1 的权限矩阵）。
 */
public enum ToolRiskLevel {

    READ_ONLY("只读，不改变数据"),
    SENSITIVE_READ("只读，但涉及个人敏感信息"),
    MUTATING("会修改数据，需要严格权限控制");

    private final String description;

    ToolRiskLevel(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
