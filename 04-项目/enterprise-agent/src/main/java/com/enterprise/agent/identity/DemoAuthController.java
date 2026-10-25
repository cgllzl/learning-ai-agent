package com.enterprise.agent.identity;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 本地演示身份入口；账号目录由后端维护，登录请求不能自定义租户或角色。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class DemoAuthController {

    private final DemoSessionService sessionService;

    public DemoAuthController(DemoSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/demo-accounts")
    public List<DemoAccountSummary> accounts() {
        return sessionService.listAccounts();
    }

    @PostMapping("/demo-login")
    public DemoLoginResponse login(@Valid @RequestBody DemoLoginRequest request) {
        return sessionService.login(request.accountId());
    }

    @GetMapping("/me")
    public DemoPrincipal me(@AuthenticationPrincipal DemoPrincipal principal) {
        return principal;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestAttribute(DemoBearerAuthenticationFilter.ACCESS_TOKEN_ATTRIBUTE)
                       String accessToken) {
        sessionService.logout(accessToken);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidLogin() {
        return Map.of("error", "INVALID_DEMO_ACCOUNT");
    }
}
