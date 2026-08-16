package com.enterprise.agent.chat;

public class AiServiceUnavailableException extends RuntimeException {

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}