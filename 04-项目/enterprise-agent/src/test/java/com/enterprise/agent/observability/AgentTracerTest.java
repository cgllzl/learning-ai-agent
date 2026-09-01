package com.enterprise.agent.observability;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTracerTest {

    @Test
    void nestedSpansHaveCorrectParentChildRelationship() {
        AgentTracer tracer = new AgentTracer();

        tracer.startSpan("AGENT:chat", Map.of("input", "查询订单 O1001"));
        tracer.startSpan("TOOL:getOrder", Map.of("arguments", "{\"orderId\":\"O1001\"}"));
        tracer.endSpan("OK", Map.of("result", "订单 O1001 ... 399"));
        tracer.endSpan("OK", Map.of());

        assertThat(tracer.spans()).hasSize(2);
        TraceSpan toolSpan = tracer.spans().stream()
                .filter(span -> span.name().equals("TOOL:getOrder"))
                .findFirst().orElseThrow();
        TraceSpan rootSpan = tracer.spans().stream()
                .filter(span -> span.name().equals("AGENT:chat"))
                .findFirst().orElseThrow();

        assertThat(toolSpan.parentSpanId()).isEqualTo(rootSpan.spanId());
        assertThat(rootSpan.parentSpanId()).isNull();
        assertThat(toolSpan.status()).isEqualTo("OK");
    }
}
