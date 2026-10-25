package com.enterprise.agent.identity;

/**
 * 第二阶段演示角色；后端用它生成权限，前端只负责显示允许的入口。
 */
public enum DemoRole {
    CUSTOMER_SERVICE,
    OPERATIONS_MANAGER,
    KNOWLEDGE_ADMIN,
    AUDITOR,
    SYSTEM_ADMIN
}
