package com.enterprise.agent.workflow.durable;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 按租户和 Agent Run 隔离的审批状态。重复申请返回同一个审批单。
 */
@Component
public class WorkflowApprovalService {

    public record WorkflowApproval(String approvalId, String tenantId, String runId, String summary) {
    }

    private final Map<String, WorkflowApproval> pending = new HashMap<>();
    private final Map<String, WorkflowApproval> approved = new HashMap<>();

    public synchronized WorkflowApproval request(String tenantId, String runId, String summary) {
        String key = key(tenantId, runId);
        WorkflowApproval existing = pending.get(key);
        if (existing != null) {
            return existing;
        }
        WorkflowApproval approval = new WorkflowApproval(
                UUID.randomUUID().toString(), tenantId, runId, summary);
        pending.put(key, approval);
        return approval;
    }

    public synchronized void approve(String tenantId, String runId) {
        String key = key(tenantId, runId);
        WorkflowApproval approval = pending.remove(key);
        if (approval == null) {
            throw new IllegalArgumentException("没有当前租户可审批的 Agent Run");
        }
        approved.put(key, approval);
    }

    public synchronized boolean isApproved(String tenantId, String runId) {
        return approved.containsKey(key(tenantId, runId));
    }

    private String key(String tenantId, String runId) {
        return tenantId + "\u0000" + runId;
    }
}
