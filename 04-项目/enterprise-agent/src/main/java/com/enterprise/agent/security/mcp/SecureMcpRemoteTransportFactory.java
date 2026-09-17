package com.enterprise.agent.security.mcp;

import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

/** 为远程 MCP 构造默认不泄露凭证的 Streamable HTTP 传输。 */
public final class SecureMcpRemoteTransportFactory {

    private SecureMcpRemoteTransportFactory() {
    }

    public static StreamableHttpMcpTransport create(McpServerRegistration server,
                                                    McpRemoteCredential credential,
                                                    Set<String> requiredScopes,
                                                    Duration timeout) {
        URI endpoint = server.resourceAudience();
        if (!server.targetHost().equalsIgnoreCase(endpoint.getHost())) {
            throw new IllegalArgumentException("MCP 登记的 targetHost 与 resourceAudience 不一致");
        }
        return StreamableHttpMcpTransport.builder()
                .url(endpoint.toString())
                // Supplier 会在每次 HTTP 请求前求值；当前凭证是固定 Token，也可在后续替换为可信刷新组件。
                // 不要把 Token 拼进 URL 或 Tool 参数。
                .customHeaders(() -> credential.authorizationHeadersFor(endpoint, requiredScopes))
                .timeout(timeout)
                // 当前 LangChain4j 版本会记录 JSON 请求体（含 Tool 参数）；安全默认关闭。
                // Token 仍主要依靠 exact audience、禁重定向和日志配置评审来保护。
                .logRequests(false)
                // Tool 结果也可能包含客户数据，默认同样不打印完整响应。
                .logResponses(false)
                // 禁止自动跟随重定向，防止 Authorization 被带到攻击者控制的地址。
                .followRedirects(false)
                .build();
    }
}
