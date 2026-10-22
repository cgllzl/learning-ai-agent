package com.enterprise.agent.workflow;

/**
 * 只给工作台提供导航建议，不返回订单或知识内容，也不声称已执行。
 */
public record WorkbenchRouteResult(
        TicketCategory category,
        WorkbenchView suggestedView,
        boolean requiresHumanReview,
        String executionStatus
) {
}
