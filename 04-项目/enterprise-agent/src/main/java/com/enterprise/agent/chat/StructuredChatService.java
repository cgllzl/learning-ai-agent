package com.enterprise.agent.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StructuredChatService {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    public StructuredChatService(OpenAiChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public JsonNode structured(String systemPrompt, List<ChatRequest.Message> messages,
                               String schemaName, String mode) {
        StructuredSchema schema = resolveSchema(schemaName);
        String effectiveSystemPrompt = joinWithHint(systemPrompt, schema.promptHint());
        List<ChatMessage> chatMessages = Messages.toLangChain4jMessages(effectiveSystemPrompt, messages);
        ResponseFormat responseFormat = resolveResponseFormat(mode, schema);

        dev.langchain4j.model.chat.request.ChatRequest chatRequest = dev.langchain4j.model.chat.request.ChatRequest.builder()
                .messages(chatMessages)
                .responseFormat(responseFormat)
                .build();

        ChatResponse response = chatModel.chat(chatRequest);
        AiMessage answer = response.aiMessage();
        if (answer == null || answer.text() == null || answer.text().isBlank()) {
            throw new IllegalStateException("模型未返回内容");
        }
        JsonNode result;
        try {
            result = objectMapper.readTree(answer.text());
        } catch (Exception e) {
            throw new IllegalStateException("模型未返回合法 JSON: " + answer.text(), e);
        }
        validateRequiredFields(result, schema.requiredFields());
        return result;
    }

    private ResponseFormat resolveResponseFormat(String mode, StructuredSchema schema) {
        String effectiveMode = (mode == null || mode.isBlank()) ? "json_object" : mode.toLowerCase();
        return switch (effectiveMode) {
            // DeepSeek 兼容：JSON 格式约束（json_object）+ prompt 强化 + 客户端按 Schema 校验
            case "json_object" -> ResponseFormat.JSON;
            // OpenAI 系原生严格模式（DeepSeek 当前不支持，会报 response_format type unavailable）
            case "json_schema" -> ResponseFormat.builder()
                    .type(ResponseFormatType.JSON)
                    .jsonSchema(schema.jsonSchema())
                    .build();
            default -> throw new IllegalArgumentException("不支持的 mode: " + mode);
        };
    }

    private String joinWithHint(String systemPrompt, String hint) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return hint;
        }
        return systemPrompt + "\n" + hint;
    }

    private void validateRequiredFields(JsonNode result, List<String> requiredFields) {
        for (String field : requiredFields) {
            if (!result.has(field) || result.get(field).isNull()) {
                throw new IllegalStateException("结构化结果缺少字段: " + field);
            }
        }
    }

    private StructuredSchema resolveSchema(String schemaName) {
        String name = (schemaName == null || schemaName.isBlank()) ? "extract" : schemaName.toLowerCase();
        return switch (name) {
            case "extract" -> new StructuredSchema(
                    JsonSchema.builder()
                            .name("extract_info")
                            .rootElement(JsonObjectSchema.builder()
                                    .addStringProperty("summary", "内容摘要")
                                    .addEnumProperty("sentiment", List.of("positive", "neutral", "negative"))
                                    .addProperty("keywords",
                                            JsonArraySchema.builder()
                                                    .items(JsonStringSchema.builder().build())
                                                    .build())
                                    .required("summary", "keywords", "sentiment")
                                    .build())
                            .build(),
                    "输出必须是一个 JSON 对象，包含三个字段：summary（字符串，内容摘要）、keywords（字符串数组）、sentiment（枚举，取值为 positive/neutral/negative 之一）。不要输出其他内容。",
                    List.of("summary", "keywords", "sentiment"));
            case "ticket" -> new StructuredSchema(
                    JsonSchema.builder()
                            .name("ticket_info")
                            .rootElement(JsonObjectSchema.builder()
                                    .addEnumProperty("category", List.of("question", "bug", "suggestion"))
                                    .addEnumProperty("priority", List.of("high", "medium", "low"))
                                    .addBooleanProperty("needs_human", "是否需要人工介入")
                                    .addStringProperty("reply", "给用户的回复")
                                    .required("category", "priority", "needs_human", "reply")
                                    .build())
                            .build(),
                    "输出必须是一个 JSON 对象，包含四个字段：category（枚举 question/bug/suggestion）、priority（枚举 high/medium/low）、needs_human（布尔）、reply（字符串）。不要输出其他内容。",
                    List.of("category", "priority", "needs_human", "reply"));
            default -> throw new IllegalArgumentException("不支持的 schema: " + schemaName);
        };
    }

    private record StructuredSchema(JsonSchema jsonSchema, String promptHint, List<String> requiredFields) {
    }
}