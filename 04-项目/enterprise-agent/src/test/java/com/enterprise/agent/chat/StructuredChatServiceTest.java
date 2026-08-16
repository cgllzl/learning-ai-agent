package com.enterprise.agent.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StructuredChatServiceTest {

    private final OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StructuredChatService service = new StructuredChatService(chatModel, objectMapper);

    @BeforeEach
    void setUp() {
        when(chatModel.chat(any(dev.langchain4j.model.chat.request.ChatRequest.class))).thenReturn(
                ChatResponse.builder().aiMessage(new AiMessage("""
                        {"summary":"Java 21 的新特性","keywords":["虚拟线程","Record","模式匹配"],"sentiment":"positive"}
                        """)).build());
    }

    @Test
    void returnsParsedJsonNodeInDefaultJsonObjectMode() throws Exception {
        JsonNode result = service.structured(
                "你是信息抽取助手",
                List.of(new ChatRequest.Message("user", "Java 21 新增了虚拟线程")),
                null,
                null);

        assertThat(result.get("summary").asText()).contains("Java 21");
        assertThat(result.get("sentiment").asText()).isEqualTo("positive");
        assertThat(result.get("keywords").isArray()).isTrue();
    }

    @Test
    void defaultModeUsesJsonObjectFormatWithoutSchema() {
        service.structured(null, List.of(new ChatRequest.Message("user", "hi")), null, null);

        ArgumentCaptor<dev.langchain4j.model.chat.request.ChatRequest> captor =
                ArgumentCaptor.forClass(dev.langchain4j.model.chat.request.ChatRequest.class);
        verify(chatModel).chat((dev.langchain4j.model.chat.request.ChatRequest) captor.capture());

        ResponseFormat format = captor.getValue().responseFormat();
        assertThat(format.type()).isEqualTo(ResponseFormatType.JSON);
        assertThat(format.jsonSchema()).isNull();
    }

    @Test
    void jsonSchemaModeAttachesSchemaToRequest() {
        when(chatModel.chat(any(dev.langchain4j.model.chat.request.ChatRequest.class))).thenReturn(
                ChatResponse.builder().aiMessage(new AiMessage("""
                        {"category":"question","priority":"medium","needs_human":false,"reply":"好的"}
                        """)).build());

        service.structured(null, List.of(new ChatRequest.Message("user", "hi")), "ticket", "json_schema");

        ArgumentCaptor<dev.langchain4j.model.chat.request.ChatRequest> captor =
                ArgumentCaptor.forClass(dev.langchain4j.model.chat.request.ChatRequest.class);
        verify(chatModel).chat((dev.langchain4j.model.chat.request.ChatRequest) captor.capture());

        ResponseFormat format = captor.getValue().responseFormat();
        assertThat(format.type()).isEqualTo(ResponseFormatType.JSON);
        assertThat(format.jsonSchema()).isNotNull();
        assertThat(format.jsonSchema().name()).isEqualTo("ticket_info");
    }

    @Test
    void classifySchemaAttachesCorrectSchema() {
        when(chatModel.chat(any(dev.langchain4j.model.chat.request.ChatRequest.class))).thenReturn(
                ChatResponse.builder().aiMessage(new AiMessage("""
                        {"category":"tech","tags":["AI","Java"],"confidence":0.95}
                        """)).build());

        service.structured(null, List.of(new ChatRequest.Message("user", "这是一篇讲 Java 21 的文章")), "classify", "json_schema");

        ArgumentCaptor<dev.langchain4j.model.chat.request.ChatRequest> captor =
                ArgumentCaptor.forClass(dev.langchain4j.model.chat.request.ChatRequest.class);
        verify(chatModel).chat((dev.langchain4j.model.chat.request.ChatRequest) captor.capture());

        ResponseFormat format = captor.getValue().responseFormat();
        assertThat(format.jsonSchema().name()).isEqualTo("classify_content");
    }

    @Test
    void resumeSchemaAttachesCorrectSchema() {
        when(chatModel.chat(any(dev.langchain4j.model.chat.request.ChatRequest.class))).thenReturn(
                ChatResponse.builder().aiMessage(new AiMessage("""
                        {"name":"张三","years":5,"skills":["Java","Spring"],"education":["本科"]}
                        """)).build());

        JsonNode result = service.structured(
                null, List.of(new ChatRequest.Message("user", "我叫张三，五年 Java 经验，本科")), "resume", null);

        assertThat(result.get("name").asText()).isEqualTo("张三");
        assertThat(result.get("years").asInt()).isEqualTo(5);
    }

    @Test
    void productSchemaRejectsMissingField() {
        when(chatModel.chat(any(dev.langchain4j.model.chat.request.ChatRequest.class))).thenReturn(
                ChatResponse.builder().aiMessage(new AiMessage("""
                        {"name":"机械键盘","category":"外设","price":399}
                        """)).build());

        assertThatThrownBy(() -> service.structured(
                null, List.of(new ChatRequest.Message("user", "机械键盘 399 元")), "product", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("缺少字段");
    }

    @Test
    void rejectsUnknownSchema() {
        assertThatThrownBy(() -> service.structured(
                null, List.of(new ChatRequest.Message("user", "hi")), "unknown", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的 schema");
    }

    @Test
    void rejectsUnknownMode() {
        assertThatThrownBy(() -> service.structured(
                null, List.of(new ChatRequest.Message("user", "hi")), null, "xml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的 mode");
    }

    @Test
    void rejectsNonJsonModelReply() {
        when(chatModel.chat(any(dev.langchain4j.model.chat.request.ChatRequest.class))).thenReturn(
                ChatResponse.builder().aiMessage(new AiMessage("这不是 JSON")).build());

        assertThatThrownBy(() -> service.structured(
                null, List.of(new ChatRequest.Message("user", "hi")), null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未返回合法 JSON");
    }

    @Test
    void rejectsResultMissingRequiredField() {
        when(chatModel.chat(any(dev.langchain4j.model.chat.request.ChatRequest.class))).thenReturn(
                ChatResponse.builder().aiMessage(new AiMessage("""
                        {"summary":"只有摘要","keywords":[]}
                        """)).build());

        assertThatThrownBy(() -> service.structured(
                null, List.of(new ChatRequest.Message("user", "hi")), "extract", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("缺少字段");
    }
}