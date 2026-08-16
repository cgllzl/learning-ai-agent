package com.enterprise.agent.chat;

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.NonRetriableException;
import dev.langchain4j.exception.RetriableException;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * AI 调用容错：指数退避重试（可重试异常）+ 备用模型降级 + 全部失败抛 AiServiceUnavailableException。
 * 超时由模型 Bean 的 timeout 配置承担（OpenAiChatModel.timeout）。
 */
@Component
public class ResilientCaller {

    private static final long BASE_BACKOFF_MS = 500;
    private static final long MAX_BACKOFF_MS = 5000;

    private final DeepSeekProperties properties;
    private final OpenAiChatModel primaryModel;
    private final OpenAiChatModel fallbackModel;

    public ResilientCaller(DeepSeekProperties properties,
                           @Qualifier("openAiChatModel") OpenAiChatModel primaryModel,
                           @Qualifier("openAiChatModelFallback") OpenAiChatModel fallbackModel) {
        this.properties = properties;
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
    }

    public <T> T callWithFallback(Function<OpenAiChatModel, T> modelAction) {
        try {
            return retry(() -> modelAction.apply(primaryModel));
        } catch (RuntimeException primaryFailure) {
            // 非可重试错误（参数/认证等）直接抛出，降级没有意义
            if (!isRetryable(primaryFailure)) {
                throw primaryFailure;
            }
            try {
                return retry(() -> modelAction.apply(fallbackModel));
            } catch (RuntimeException fallbackFailure) {
                throw new AiServiceUnavailableException(
                        "主模型与备用模型均调用失败（已重试 " + properties.maxRetries() + " 次）: "
                                + fallbackFailure.getMessage(),
                        fallbackFailure);
            }
        }
    }

    private <T> T retry(Supplier<T> action) {
        int attempts = 0;
        while (true) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                attempts++;
                if (attempts > properties.maxRetries() || !isRetryable(e)) {
                    throw e;
                }
                sleep(backoffMs(attempts));
            }
        }
    }

    private long backoffMs(int attempt) {
        long backoff = BASE_BACKOFF_MS * (1L << (attempt - 1));
        return Math.min(backoff, MAX_BACKOFF_MS);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceUnavailableException("重试等待被中断", e);
        }
    }

    private boolean isRetryable(Throwable e) {
        if (e instanceof NonRetriableException) {
            return false;
        }
        if (e instanceof RetriableException) {
            return true;
        }
        if (e instanceof HttpException http) {
            return http.statusCode() >= 500 || http.statusCode() == 429;
        }
        if (e instanceof IOException) {
            return true;
        }
        return false;
    }
}