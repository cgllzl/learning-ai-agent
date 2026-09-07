package com.enterprise.agent.workflow.durable;

/**
 * 外部通知边界。通知属于已经发出就很难撤回的副作用。
 */
public interface NotificationGateway {

    String sendStatusChanged(String userId, String orderId, String newStatus);
}
