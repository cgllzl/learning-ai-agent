package com.enterprise.agent.workbench;

import com.enterprise.agent.identity.DemoPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Day 3 的最小租户隔离知识目录；Day 5 将替换为带权限的文档检索。 */
@Service
public class WorkbenchDemoKnowledge {

    private static final Map<String, WorkbenchCitation> BY_TENANT = Map.of(
            "tenant-a", new WorkbenchCitation("tenant-a-after-sales", "华东零售售后规则（演示）",
                    "华东零售：签收后 7 天内可发起售后申请；退款须经运营主管审批。"),
            "tenant-b", new WorkbenchCitation("tenant-b-after-sales", "华南供应链售后规则（演示）",
                    "华南供应链：签收后 3 天内须提交售后工单；退款须经运营主管审批。")
    );
    private static final Map<String, String> TENANT_NAMES = Map.of(
            "tenant-a", "华东零售", "tenant-b", "华南供应链");

    public List<WorkbenchCitation> find(DemoPrincipal principal, String question) {
        // 先按受信租户取文档，再做演示级关键词匹配，绝不跨租户扫描后再过滤。
        WorkbenchCitation citation = BY_TENANT.get(principal.tenantId());
        boolean asksAboutOtherTenant = TENANT_NAMES.entrySet().stream()
                .anyMatch(entry -> !entry.getKey().equals(principal.tenantId())
                        && question.contains(entry.getValue()));
        if (citation == null || asksAboutOtherTenant || !(question.contains("售后") || question.contains("退货")
                || question.contains("退款") || question.contains("签收"))) {
            return List.of();
        }
        return List.of(citation);
    }
}
