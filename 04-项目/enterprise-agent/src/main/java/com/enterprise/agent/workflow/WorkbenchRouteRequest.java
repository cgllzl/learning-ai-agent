package com.enterprise.agent.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 工作台路由预判的输入；这里只接受用户问题，不接收由前端自报的权限或租户。
 */
public record WorkbenchRouteRequest(
        @NotBlank(message = "message 不能为空")
        @Size(max = 2000, message = "message 最多 2000 字符")
        String message
) {
}
