package com.enterprise.agent.security;

import com.enterprise.agent.agent.MockOrderData;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * 带「审计日志 + 人工审批」的订单工具。
 * getOrder 直接执行并记审计；updateOrderStatus 必须先有人工审批，否则只生成审批单。
 */
public class ApprovalAwareOrderTools {

    private final MockOrderData data;
    private final AuditLogService auditLog;
    private final HumanApprovalService approvalService;

    public ApprovalAwareOrderTools(MockOrderData data,
                                   AuditLogService auditLog,
                                   HumanApprovalService approvalService) {
        this.data = data;
        this.auditLog = auditLog;
        this.approvalService = approvalService;
    }

    @Tool("根据订单号查询订单信息")
    public String getOrder(@P("订单号，例如 O1001") String orderId) {
        SecuritySubject subject = TenantContext.current();
        String result = data.findOrderById(orderId)
                .map(order -> "订单 " + order.id()
                        + "：用户 " + order.userId()
                        + "，商品 " + order.productId()
                        + "，金额 " + order.amount() + " 元，状态 " + order.status())
                .orElse("未找到订单 " + orderId);
        auditLog.record(subject, "getOrder", orderId, result, AuditStatus.SUCCESS);
        return result;
    }

    @Tool("修改订单状态。只有 PENDING 的订单可以修改；新状态必须是 PAID、SHIPPED、DELIVERED 或 CANCELLED 之一")
    public String updateOrderStatus(@P("订单号") String orderId,
                                    @P("新状态") String newStatus) {
        SecuritySubject subject = TenantContext.current();
        String arguments = orderId + " -> " + newStatus;

        if (!approvalService.isApproved(orderId, newStatus)) {
            HumanApprovalService.PendingApproval approval =
                    approvalService.requestApproval(orderId, newStatus);
            String message = "订单 " + orderId + " 改为 " + newStatus + " 需要人工审批，审批单号：" + approval.approvalId();
            auditLog.record(subject, "updateOrderStatus", arguments, message, AuditStatus.PENDING_APPROVAL);
            return message;
        }

        MockOrderData.Order order = data.findOrderById(orderId).orElse(null);
        if (order == null) {
            String message = "未找到订单 " + orderId;
            auditLog.record(subject, "updateOrderStatus", arguments, message, AuditStatus.DENIED);
            return message;
        }
        if (!"PENDING".equals(order.status())) {
            String message = "订单 " + orderId + " 当前状态为 " + order.status() + "，只有 PENDING 状态可以修改";
            auditLog.record(subject, "updateOrderStatus", arguments, message, AuditStatus.DENIED);
            return message;
        }

        data.updateOrderStatus(orderId, newStatus);
        String message = "订单 " + orderId + " 状态已更新为 " + newStatus;
        auditLog.record(subject, "updateOrderStatus", arguments, message, AuditStatus.SUCCESS);
        return message;
    }
}
