package com.enterprise.agent.chat;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final StreamingChatService streamingChatService;
    private final StructuredChatService structuredChatService;

    public ChatController(ChatService chatService,
                          StreamingChatService streamingChatService,
                          StructuredChatService structuredChatService) {
        this.chatService = chatService;
        this.streamingChatService = streamingChatService;
        this.structuredChatService = structuredChatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String reply = chatService.chat(request.systemPrompt(), request.messages());
        return new ChatResponse(reply);
    }

    @PostMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request, jakarta.servlet.http.HttpServletResponse httpResponse) {
        httpResponse.setCharacterEncoding("UTF-8");
        SseEmitter emitter = new SseEmitter(0L);
        streamingChatService.stream(
                request.systemPrompt(),
                request.messages(),
                partial -> send(emitter, partial),
                () -> {
                    send(emitter, "[DONE]");
                    emitter.complete();
                },
                error -> {
                    send(emitter, "[ERROR] " + error.getMessage());
                    emitter.completeWithError(error);
                });
        return emitter;
    }

    @PostMapping("/structured")
    public StructuredChatResponse chatStructured(@Valid @RequestBody StructuredChatRequest request) {
        return new StructuredChatResponse(
                structuredChatService.structured(request.systemPrompt(), request.messages(), request.schema(), request.mode()));
    }

    private void send(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}