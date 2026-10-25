package com.enterprise.agent.workflow;

import jakarta.validation.Valid;
import com.enterprise.agent.identity.DemoPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Map;

/**
 * Day 1 的工作台 API 契约：只做路由预判，订单/RAG/审批在各自受控接口执行。
 */
@RestController
@RequestMapping("/api/v1/workbench")
public class WorkbenchRouteController {

    private final WorkbenchRoutingService routingService;

    public WorkbenchRouteController(WorkbenchRoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping("/route")
    public WorkbenchRouteResult route(@AuthenticationPrincipal DemoPrincipal principal,
                                      @Valid @RequestBody WorkbenchRouteRequest request) {
        // principal 来自后端会话 Filter；不读取前端自报的 userId、tenantId 或 roles。
        return routingService.route(request.message());
    }

    // 校验失败只返回固定错误码，避免把用户问题原文写入错误响应或默认异常日志。
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidMessage() {
        return Map.of("error", "INVALID_MESSAGE");
    }
}
