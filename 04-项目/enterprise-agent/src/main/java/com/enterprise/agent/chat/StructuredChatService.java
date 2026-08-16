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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StructuredChatService {

    private final ResilientCaller resilientCaller;
    private final ObjectMapper objectMapper;

    public StructuredChatService(ResilientCaller resilientCaller, ObjectMapper objectMapper) {
        this.resilientCaller = resilientCaller;
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

        ChatResponse response = resilientCaller.callWithFallback(model -> model.chat(chatRequest));
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
            // 场景 1：信息抽取（客户反馈 → 摘要/情绪/关键词）
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

            // 场景 2：工单/客服分类
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

            // 场景 3：内容分类打标
            case "classify" -> new StructuredSchema(
                    JsonSchema.builder()
                            .name("classify_content")
                            .rootElement(JsonObjectSchema.builder()
                                    .addEnumProperty("category", List.of("news", "tech", "life", "other"))
                                    .addProperty("tags",
                                            JsonArraySchema.builder()
                                                    .items(JsonStringSchema.builder().build())
                                                    .build())
                                    .addNumberProperty("confidence", "置信度，0 到 1 之间")
                                    .required("category", "tags", "confidence")
                                    .build())
                            .build(),
                    "输出必须是一个 JSON 对象，包含三个字段：category（枚举 news/tech/life/other）、tags（字符串数组）、confidence（数字，0 到 1 之间）。不要输出其他内容。",
                    List.of("category", "tags", "confidence"));

            // 场景 4：简历解析（非结构化文本 → 结构化字段）
            case "resume" -> new StructuredSchema(
                    JsonSchema.builder()
                            .name("resume_parse")
                            .rootElement(JsonObjectSchema.builder()
                                    .addStringProperty("name", "姓名")
                                    .addNumberProperty("years", "工作年限")
                                    .addProperty("skills",
                                            JsonArraySchema.builder()
                                                    .items(JsonStringSchema.builder().build())
                                                    .build())
                                    .addProperty("education",
                                            JsonArraySchema.builder()
                                                    .items(JsonStringSchema.builder().build())
                                                    .build())
                                    .required("name", "years", "skills", "education")
                                    .build())
                            .build(),
                    "输出必须是一个 JSON 对象，包含四个字段：name（字符串，姓名）、years（数字，工作年限）、skills（字符串数组，技能）、education（字符串数组，教育经历）。不要输出其他内容。",
                    List.of("name", "years", "skills", "education"));

            // 场景 5：商品属性抽取（电商）
            case "product" -> new StructuredSchema(
                    JsonSchema.builder()
                            .name("product_parse")
                            .rootElement(JsonObjectSchema.builder()
                                    .addStringProperty("name", "商品名称")
                                    .addStringProperty("category", "商品类目")
                                    .addNumberProperty("price", "价格")
                                    .addEnumProperty("stock_status", List.of("in_stock", "out_of_stock", "unknown"))
                                    .required("name", "category", "price", "stock_status")
                                    .build())
                            .build(),
                    "输出必须是一个 JSON 对象，包含四个字段：name（字符串，商品名称）、category（字符串，商品类目）、price（数字，价格）、stock_status（枚举 in_stock/out_of_stock/unknown）。不要输出其他内容。",
                    List.of("name", "category", "price", "stock_status"));

            default -> throw new IllegalArgumentException("不支持的 schema: " + schemaName);
        };
    }

    private record StructuredSchema(JsonSchema jsonSchema, String promptHint, List<String> requiredFields) {
    }
}