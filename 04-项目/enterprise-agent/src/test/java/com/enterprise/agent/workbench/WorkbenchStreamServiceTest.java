package com.enterprise.agent.workbench;

import com.enterprise.agent.chat.StreamingChatService;
import com.enterprise.agent.identity.DemoPrincipal;
import com.enterprise.agent.identity.DemoRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WorkbenchStreamServiceTest {

    private final StreamingChatService model = mock(StreamingChatService.class);
    private final WorkbenchStreamService service = new WorkbenchStreamService(model, new WorkbenchDemoKnowledge());

    @Test
    void tenantScopedKnowledgeStreamsStepsCitationAndOnlyOneTerminalEvent() {
        doAnswer(invocation -> {
            Consumer<String> partial = invocation.getArgument(2);
            Runnable complete = invocation.getArgument(3);
            partial.accept("按规则[1]，");
            partial.accept("7天内申请。");
            complete.run();
            complete.run(); // SDK 若重复回调，工作台不能重复发送终态。
            return null;
        }).when(model).stream(anyString(), anyList(), any(), any(), any());

        List<WorkbenchStreamEvent> events = new ArrayList<>();
        service.stream(principal("tenant-a"),
                new WorkbenchQueryRequest("售后期限是什么？", WorkbenchQueryRequest.Mode.KNOWLEDGE),
                sink(events));

        assertThat(events).extracting(WorkbenchStreamEvent::type)
                .containsExactly("step", "step", "citations", "step", "delta", "delta", "done");
        assertThat(events).extracting(WorkbenchStreamEvent::sequence).containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(events).extracting(WorkbenchStreamEvent::traceId).containsOnly(events.getFirst().traceId());
        assertThat(events.get(2).citations()).extracting(WorkbenchCitation::sourceId)
                .containsExactly("tenant-a-after-sales");
        assertThat(events.getLast().status()).isEqualTo("COMPLETED");

        List<WorkbenchStreamEvent> otherTenant = new ArrayList<>();
        service.stream(principal("tenant-b"),
                new WorkbenchQueryRequest("售后期限是什么？", WorkbenchQueryRequest.Mode.KNOWLEDGE),
                sink(otherTenant));
        assertThat(otherTenant.get(2).citations()).extracting(WorkbenchCitation::sourceId)
                .containsExactly("tenant-b-after-sales");
    }

    @Test
    void missingKnowledgeReturnsHonestAnswerWithoutCallingModel() {
        List<WorkbenchStreamEvent> events = new ArrayList<>();
        service.stream(principal("tenant-a"),
                new WorkbenchQueryRequest("发票在哪里？", WorkbenchQueryRequest.Mode.KNOWLEDGE),
                sink(events));

        assertThat(events).extracting(WorkbenchStreamEvent::type)
                .containsExactly("step", "step", "citations", "delta", "done");
        assertThat(events.get(2).citations()).isEmpty();
        assertThat(events.get(3).text()).isEqualTo("资料中没有相关内容。");
        verify(model, never()).stream(anyString(), anyList(), any(), any(), any());
    }

    @Test
    void explicitOtherTenantQuestionDoesNotAttachCurrentTenantDocument() {
        List<WorkbenchStreamEvent> events = new ArrayList<>();
        service.stream(principal("tenant-b"),
                new WorkbenchQueryRequest("华东零售售后期限是什么？", WorkbenchQueryRequest.Mode.KNOWLEDGE),
                sink(events));

        assertThat(events.get(2).citations()).isEmpty();
        assertThat(events.get(3).text()).isEqualTo("资料中没有相关内容。");
        verify(model, never()).stream(anyString(), anyList(), any(), any(), any());
    }

    @Test
    void closedClientDoesNotReceiveLateModelCallbacks() {
        AtomicBoolean open = new AtomicBoolean(true);
        List<WorkbenchStreamEvent> events = new ArrayList<>();
        doAnswer(invocation -> {
            Consumer<String> partial = invocation.getArgument(2);
            Runnable complete = invocation.getArgument(3);
            partial.accept("第一段");
            open.set(false);
            partial.accept("迟到的第二段");
            complete.run();
            return null;
        }).when(model).stream(anyString(), anyList(), any(), any(), any());

        service.stream(principal("tenant-a"),
                new WorkbenchQueryRequest("解释流式回答", WorkbenchQueryRequest.Mode.GENERAL),
                new WorkbenchStreamService.EventSink() {
                    public boolean isOpen() { return open.get(); }
                    public void send(WorkbenchStreamEvent event) { events.add(event); }
                });

        assertThat(events).extracting(WorkbenchStreamEvent::text).doesNotContain("迟到的第二段");
        assertThat(events).extracting(WorkbenchStreamEvent::type).doesNotContain("done");
    }

    @Test
    void modelFailureReturnsFixedCodeAndOneFailedTerminalEvent() {
        doAnswer(invocation -> {
            Consumer<Throwable> fail = invocation.getArgument(4);
            fail.accept(new RuntimeException("internal secret"));
            fail.accept(new RuntimeException("duplicate"));
            return null;
        }).when(model).stream(anyString(), anyList(), any(), any(), any());
        List<WorkbenchStreamEvent> events = new ArrayList<>();

        service.stream(principal("tenant-a"),
                new WorkbenchQueryRequest("解释流式回答", WorkbenchQueryRequest.Mode.GENERAL), sink(events));

        assertThat(events.getLast().status()).isEqualTo("FAILED");
        assertThat(events.getLast().errorCode()).isEqualTo("MODEL_UNAVAILABLE");
        assertThat(events).extracting(WorkbenchStreamEvent::type).filteredOn("done"::equals).hasSize(1);
        assertThat(events.toString()).doesNotContain("internal secret", "duplicate");
    }

    private DemoPrincipal principal(String tenantId) {
        return new DemoPrincipal("user-1", "客服", tenantId, "演示租户", Set.of(DemoRole.CUSTOMER_SERVICE));
    }

    private WorkbenchStreamService.EventSink sink(List<WorkbenchStreamEvent> events) {
        return new WorkbenchStreamService.EventSink() {
            public boolean isOpen() { return true; }
            public void send(WorkbenchStreamEvent event) { events.add(event); }
        };
    }
}
