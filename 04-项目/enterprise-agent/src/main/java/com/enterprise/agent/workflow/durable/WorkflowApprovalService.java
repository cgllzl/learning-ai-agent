package com.enterprise.agent.workflow.durable;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;
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
    private final AuditLogService auditLog;

    public WorkflowApprovalService(AuditLogService auditLog) {
        this.auditLog = auditLog;
    }

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

    public synchronized void approve(SecuritySubject approver, String runId) {
        String key = key(approver.tenantId(), runId);
        boolean hasApprovalRole = approver.hasRole("ORDER_ADMIN")
                || approver.hasRole("SUPERVISOR");
        if (!hasApprovalRole) {
            auditLog.record(approver, "approveAgentRun", "runId=" + runId,
                    "当前角色无审批权限", AuditStatus.BLOCKED);
            throw new SecurityException("当前角色无审批权限");
        }
        WorkflowApproval approval = pending.remove(key);
        if (approval == null) {
            auditLog.record(approver, "approveAgentRun", "runId=" + runId,
                    "没有当前租户可审批的 Agent Run", AuditStatus.BLOCKED);
            throw new IllegalArgumentException("没有当前租户可审批的 Agent Run");
        }
        approved.put(key, approval);
        auditLog.record(approver, "approveAgentRun", "runId=" + runId,
                "APPROVED", AuditStatus.SUCCESS);
    }

    public synchronized boolean isApproved(String tenantId, String runId) {
        return approved.containsKey(key(tenantId, runId));
    }

    private String key(String tenantId, String runId) {
        return tenantId + "\u0000" + runId;
    }
}
