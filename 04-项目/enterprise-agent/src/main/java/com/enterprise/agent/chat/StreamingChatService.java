package com.enterprise.agent.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class StreamingChatService {

    private final OpenAiStreamingChatModel streamingChatModel;

    public StreamingChatService(OpenAiStreamingChatModel streamingChatModel) {
        this.streamingChatModel = streamingChatModel;
    }

    public void stream(String systemPrompt, List<ChatRequest.Message> messages,
                       Consumer<String> onPartial, Runnable onComplete, Consumer<Throwable> onError) {
        List<ChatMessage> chatMessages = buildMessages(systemPrompt, messages);
        streamingChatModel.chat(chatMessages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                onPartial.accept(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                onComplete.run();
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        });
    }

    private List<ChatMessage> buildMessages(String systemPrompt, List<ChatRequest.Message> messages) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            chatMessages.add(SystemMessage.from(systemPrompt));
        }
        for (ChatRequest.Message message : messages) {
            chatMessages.add(toLangChain4jMessage(message));
        }
        return chatMessages;
    }

    private ChatMessage toLangChain4jMessage(ChatRequest.Message message) {
        return switch (message.role().toLowerCase()) {
            case "system" -> SystemMessage.from(message.content());
            case "user" -> UserMessage.from(message.content());
            case "assistant" -> new AiMessage(message.content());
            default -> throw new IllegalArgumentException("不支持的 role: " + message.role());
        };
    }
}