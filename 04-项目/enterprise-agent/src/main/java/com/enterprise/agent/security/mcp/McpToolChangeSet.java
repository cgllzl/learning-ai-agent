package com.enterprise.agent.security.mcp;

import java.util.Set;

/** 工具目录相对已审核清单的变化；新增、删除或变更都必须重新审核，不能自动忽略。 */
public record McpToolChangeSet(
        Set<String> added,
        Set<String> removed,
        Set<String> changed,
        Set<String> unchanged
) {
    public McpToolChangeSet {
        added = Set.copyOf(added);
        removed = Set.copyOf(removed);
        changed = Set.copyOf(changed);
        unchanged = Set.copyOf(unchanged);
    }

    public boolean requiresReview() {
        // 删除也可能表示第三方能力退化、被替换或目录遭篡改，因此任何目录变化都要告警复核。
        return !added.isEmpty() || !removed.isEmpty() || !changed.isEmpty();
    }
}
