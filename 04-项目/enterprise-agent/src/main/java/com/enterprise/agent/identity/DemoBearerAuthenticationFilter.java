package com.enterprise.agent.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 从后端会话解析 Bearer Token，并把受信身份放进 Spring SecurityContext。
 */
public class DemoBearerAuthenticationFilter extends OncePerRequestFilter {

    public static final String ACCESS_TOKEN_ATTRIBUTE =
            "com.enterprise.agent.identity.demoAccessToken";

    private final DemoSessionService sessionService;

    public DemoBearerAuthenticationFilter(DemoSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // SSE 完成时会再次分派请求；第二次也要从 Bearer 会话恢复 SecurityContext。
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = bearerToken(request.getHeader("Authorization"));
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            sessionService.resolve(token).ifPresent(principal -> {
                List<SimpleGrantedAuthority> authorities = principal.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .toList();
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        principal, token, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute(ACCESS_TOKEN_ATTRIBUTE, token);
            });
        }
        filterChain.doFilter(request, response);
    }

    private String bearerToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }
}
