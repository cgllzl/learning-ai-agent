package com.enterprise.agent.workbench;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 查询模式由页面选择；身份和租户只从已验证的会话读取。 */
public record WorkbenchQueryRequest(
        @NotBlank @Size(max = 2000) String message,
        @NotNull Mode mode
) {
    public enum Mode { GENERAL, KNOWLEDGE }
}
