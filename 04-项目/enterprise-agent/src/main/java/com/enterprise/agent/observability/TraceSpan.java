package com.enterprise.agent.observability;

import java.util.Map;

/**
 * 一段追踪 Span：记录一次动作的开始、结束、耗时、状态和属性。
 */
public record TraceSpan(
        String spanId,
        String parentSpanId,
        String name,
        long startNanos,
        long durationNanos,
        String status,
        Map<String, String> attributes) {
}
