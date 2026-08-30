package com.enterprise.agent.security;

/**
 * 检测到疑似提示词注入时抛出。
 */
public class PromptInjectionBlockedException extends RuntimeException {

    public PromptInjectionBlockedException(String message) {
        super(message);
    }
}
