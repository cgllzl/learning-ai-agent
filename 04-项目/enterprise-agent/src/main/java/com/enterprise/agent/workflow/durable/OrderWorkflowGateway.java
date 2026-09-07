package com.enterprise.agent.workflow.durable;

/**
 * 订单系统边界。生产环境可替换成数据库或远程订单服务。
 */
public interface OrderWorkflowGateway {

    String currentStatus(String orderId);

    String updateStatus(String orderId, String newStatus);
}
