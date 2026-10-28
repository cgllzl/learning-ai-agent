package com.enterprise.agent.workbench;

import java.util.List;

/** 每个 SSE 事件都带 traceId 与递增序号，方便页面显示和排查。 */
public record WorkbenchStreamEvent(
        String type,
        String traceId,
        int sequence,
        String text,
        List<WorkbenchCitation> citations,
        String status,
        String errorCode
) {
}
