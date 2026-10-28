package com.enterprise.agent.workbench;

import com.enterprise.agent.identity.DemoPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/v1/workbench")
public class WorkbenchStreamController {

    private final WorkbenchStreamService streamService;

    public WorkbenchStreamController(WorkbenchStreamService streamService) {
        this.streamService = streamService;
    }

    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter query(@AuthenticationPrincipal DemoPrincipal principal,
                            @Valid @RequestBody WorkbenchQueryRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);
        AtomicBoolean open = new AtomicBoolean(true);
        emitter.onCompletion(() -> open.set(false));
        emitter.onTimeout(() -> {
            open.set(false);
            emitter.complete();
        });
        emitter.onError(error -> open.set(false));

        streamService.stream(principal, request, new WorkbenchStreamService.EventSink() {
            @Override
            public boolean isOpen() {
                return open.get();
            }

            @Override
            public void send(WorkbenchStreamEvent event) {
                if (!open.get()) return;
                try {
                    // event 名称方便浏览器分流；JSON 数据保留 traceId、序号和统一终态。
                    emitter.send(SseEmitter.event().name(event.type()).data(event));
                    if ("done".equals(event.type()) && open.compareAndSet(true, false)) {
                        emitter.complete();
                    }
                } catch (IOException | IllegalStateException disconnected) {
                    // 浏览器停止/断线后不继续向同一连接写；上游模型请求可能仍在运行。
                    open.set(false);
                    emitter.complete();
                }
            }
        });
        return emitter;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidQuery() {
        return Map.of("error", "INVALID_QUERY");
    }
}
