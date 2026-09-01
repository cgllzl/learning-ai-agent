package com.enterprise.agent.observability;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存版链路追踪器（Week 6 Day 3）。
 * 每次 startSpan/endSpan 构成一段 Span，父子关系由当前线程的栈维护。
 */
public class AgentTracer {

    private record ActiveSpan(
            String spanId,
            String parentSpanId,
            String name,
            long startNanos,
            Map<String, String> attributes) {
    }

    private final AtomicLong counter = new AtomicLong();
    private final Map<String, TraceSpan> completedSpans = new LinkedHashMap<>();
    private final ThreadLocal<Deque<ActiveSpan>> stack = ThreadLocal.withInitial(ArrayDeque::new);

    public void startSpan(String name, Map<String, String> attributes) {
        Deque<ActiveSpan> currentStack = stack.get();
        ActiveSpan parent = currentStack.peek();
        ActiveSpan span = new ActiveSpan(
                "span-" + counter.incrementAndGet(),
                parent == null ? null : parent.spanId(),
                name,
                System.nanoTime(),
                attributes == null ? Map.of() : attributes);
        currentStack.push(span);
    }

    public void endSpan(String status, Map<String, String> attributes) {
        Deque<ActiveSpan> currentStack = stack.get();
        ActiveSpan active = currentStack.pop();
        long durationNanos = System.nanoTime() - active.startNanos();

        Map<String, String> merged = new LinkedHashMap<>(active.attributes());
        if (attributes != null) {
            merged.putAll(attributes);
        }

        TraceSpan span = new TraceSpan(
                active.spanId(),
                active.parentSpanId(),
                active.name(),
                active.startNanos(),
                durationNanos,
                status,
                Map.copyOf(merged));
        completedSpans.put(span.spanId(), span);
    }

    public List<TraceSpan> spans() {
        return List.copyOf(completedSpans.values());
    }
}
