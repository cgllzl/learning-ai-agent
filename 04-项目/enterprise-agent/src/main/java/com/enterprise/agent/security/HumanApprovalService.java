package com.enterprise.agent.security;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 高危操作人工审批流（Day 6 演示版）。
 * 高危工具第一次调用时只生成审批单，不真正执行；人工 approve 后才放行。
 */
public class HumanApprovalService {

    public record PendingApproval(String approvalId, String orderId, String newStatus) {
    }

    private final Map<String, PendingApproval> pending = new ConcurrentHashMap<>();
    private final Set<String> granted = ConcurrentHashMap.newKeySet();

    public PendingApproval requestApproval(String orderId, String newStatus) {
        String approvalId = UUID.randomUUID().toString();
        PendingApproval approval = new PendingApproval(approvalId, orderId, newStatus);
        pending.put(key(orderId, newStatus), approval);
        return approval;
    }

    public void approve(String orderId, String newStatus) {
        String approvalKey = key(orderId, newStatus);
        pending.remove(approvalKey);
        granted.add(approvalKey);
    }

    public boolean isApproved(String orderId, String newStatus) {
        return granted.contains(key(orderId, newStatus));
    }

    public int pendingCount() {
        return pending.size();
    }

    private String key(String orderId, String newStatus) {
        return orderId + ":" + newStatus;
    }
}
