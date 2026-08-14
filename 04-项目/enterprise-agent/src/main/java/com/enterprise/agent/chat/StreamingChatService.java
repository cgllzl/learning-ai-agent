package com.enterprise.agent.chat;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Service;

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
        List<ChatMessage> chatMessages = Messages.toLangChain4jMessages(systemPrompt, messages);
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
}