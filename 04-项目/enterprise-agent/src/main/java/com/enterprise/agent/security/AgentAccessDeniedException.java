package com.enterprise.agent.security;

/**
 * 权限不足时抛出的异常。
 */
public class AgentAccessDeniedException extends RuntimeException {

    public AgentAccessDeniedException(String message) {
        super(message);
    }
}
