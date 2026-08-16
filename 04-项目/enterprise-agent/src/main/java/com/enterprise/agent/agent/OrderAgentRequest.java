package com.enterprise.agent.agent;

import jakarta.validation.constraints.NotBlank;

public record OrderAgentRequest(
        @NotBlank(message = "message 不能为空") String message
) {
}