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

    public ChatController(ChatService chatService, StreamingChatService streamingChatService) {
        this.chatService = chatService;
        this.streamingChatService = streamingChatService;
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

    private void send(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}