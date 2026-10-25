package com.enterprise.agent.identity;

import jakarta.validation.constraints.NotBlank;

public record DemoLoginRequest(
        @NotBlank(message = "accountId 不能为空") String accountId
) {
}
