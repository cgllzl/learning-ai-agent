package com.enterprise.agent.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.InternalServerException;
import dev.langchain4j.exception.InvalidRequestException;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ResilientCaller 容错逻辑的单元测试。
 *
 * <p>背景：ResilientCaller 对外提供 {@code callWithFallback(Function<OpenAiChatModel, T>)}，
 * 内部流程 = 主模型重试（指数退避）→ 备用模型重试 → 全部失败抛 AiServiceUnavailableException。
 * 本测试不联网、不花钱，用 mock 的两个模型（primary/fallback）模拟各种成功/失败场景，
 * 验证「什么情况重试、什么情况降级、什么情况直接失败」。
 *
 * <p>公共桩：primary 与 fallback 都是 mock；maxRetries = 2 表示每个模型最多重试 2 次（即最多调用 3 次）。
 */
class ResilientCallerTest {

    private static final List<ChatMessage> MESSAGES = List.of(UserMessage.from("hi"));

    private final DeepSeekProperties props =
            new DeepSeekProperties("key", "https://api.deepseek.com", "primary-model",
                    Duration.ofSeconds(30), 2, "fallback-model");
    private final OpenAiChatModel primary = mock(OpenAiChatModel.class);
    private final OpenAiChatModel fallback = mock(OpenAiChatModel.class);
    private final ResilientCaller caller = new ResilientCaller(props, primary, fallback);

    private ChatResponse okResponse() {
        return ChatResponse.builder().aiMessage(new AiMessage("ok")).build();
    }

    /**
     * 正常路径：首次调用就成功。
     * 期望：主模型只调用 1 次、备用模型完全不介入，返回模型回复。
     * 作用：保证容错包装不影响正常调用（重试/降级是"锦上添花"，不能误伤正常路径）。
     */
    @Test
    void succeedsImmediatelyOnFirstAttempt() {
        when(primary.chat(MESSAGES)).thenReturn(okResponse());

        ChatResponse response = caller.callWithFallback(model -> model.chat(MESSAGES));

        assertThat(response.aiMessage().text()).isEqualTo("ok");
        verify(primary, times(1)).chat(MESSAGES);
        verify(fallback, never()).chat(anyList());
    }

    /**
     * 可重试错误 → 重试后成功。
     * 场景：模型先抛 2 次 5xx（InternalServerException，属于 RetriableException），第 3 次成功。
     * 期望：主模型共被调用 3 次（1 次原始 + 2 次重试），重试期间按指数退避等待，备用模型不介入。
     * 作用：验证「暂时性故障」能被自动重试扛过去。
     */
    @Test
    void retriesTransientFailureThenSucceeds() {
        when(primary.chat(MESSAGES))
                .thenThrow(new InternalServerException("boom"))
                .thenThrow(new InternalServerException("boom"))
                .thenReturn(okResponse());

        ChatResponse response = caller.callWithFallback(model -> model.chat(MESSAGES));

        assertThat(response).isNotNull();
        verify(primary, times(3)).chat(MESSAGES);
        verify(fallback, never()).chat(anyList());
    }

    /**
     * 不可重试错误 → 快速失败、不重试、不降级。
     * 场景：模型抛 400（InvalidRequestException，属于 NonRetriableException）。
     * 期望：主模型只调用 1 次，异常原样抛出（不包装成 503），备用模型不介入。
     * 作用：参数/认证类错误重试和降级都没有意义，必须立刻暴露给上层，避免掩盖问题。
     */
    @Test
    void failsFastOnNonRetriableError() {
        when(primary.chat(MESSAGES)).thenThrow(new InvalidRequestException("bad request"));

        assertThatThrownBy(() -> caller.callWithFallback(model -> model.chat(MESSAGES)))
                .isInstanceOf(InvalidRequestException.class);

        verify(primary, times(1)).chat(MESSAGES);
        verify(fallback, never()).chat(anyList());
    }

    /**
     * 主模型重试耗尽 → 切备用模型降级。
     * 场景：主模型持续 5xx（重试 2 次后仍失败），备用模型一次成功。
     * 期望：最终拿到备用模型回复；备用模型被调用 1 次。
     * 作用：验证降级路径——主模型不可用时服务仍可用（这就是 fallback-model 配置的意义）。
     */
    @Test
    void fallsBackToSecondaryModelWhenPrimaryExhaustsRetries() {
        when(primary.chat(MESSAGES)).thenThrow(new InternalServerException("down"));
        when(fallback.chat(MESSAGES)).thenReturn(okResponse());

        ChatResponse response = caller.callWithFallback(model -> model.chat(MESSAGES));

        assertThat(response).isNotNull();
        verify(fallback, times(1)).chat(MESSAGES);
    }

    /**
     * 主模型与备用模型都失败 → 抛 AiServiceUnavailableException（对外即 HTTP 503）。
     * 场景：两个模型都持续 5xx。
     * 期望：不返回空结果、不静默吞错，而是抛出明确的业务异常，由异常处理器转成 503。
     * 作用：验证「最后的兜底」——明确告知调用方 AI 服务不可用，而不是返回垃圾数据。
     */
    @Test
    void throwsAiServiceUnavailableWhenBothModelsFail() {
        when(primary.chat(MESSAGES)).thenThrow(new InternalServerException("down"));
        when(fallback.chat(MESSAGES)).thenThrow(new InternalServerException("down too"));

        assertThatThrownBy(() -> caller.callWithFallback(model -> model.chat(MESSAGES)))
                .isInstanceOf(AiServiceUnavailableException.class);
    }
}