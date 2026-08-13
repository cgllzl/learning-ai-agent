package com.enterprise.agent.chat;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DeepSeekProperties.class)
public class ChatConfig {

    @Bean
    OpenAiChatModel openAiChatModel(DeepSeekProperties props) {
        return OpenAiChatModel.builder()
                .baseUrl(props.baseUrl())
                .apiKey(props.apiKey())
                .modelName(props.model())
                .build();
    }

    @Bean
    OpenAiStreamingChatModel openAiStreamingChatModel(DeepSeekProperties props) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(props.baseUrl())
                .apiKey(props.apiKey())
                .modelName(props.model())
                .build();
    }
}