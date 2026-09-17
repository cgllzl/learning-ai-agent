package com.enterprise.agent.security.mcp;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import io.modelcontextprotocol.spec.ProtocolVersions;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * 当前项目的 MCP 兼容性事实表。
 *
 * <p>规范“已经发布”不等于当前 Java 依赖“已经实现”。把能力边界写成代码，
 * 可以让测试在升级依赖时提醒我们重新核对，而不是只靠一篇可能过期的文档。</p>
 */
public final class McpCompatibilityMatrix {

    public static final String MODERN_PROTOCOL = "2026-07-28";

    private McpCompatibilityMatrix() {
    }

    public static ProjectMcpStack currentProject() {
        String langChain4jVersion = artifactVersion(
                "META-INF/maven/dev.langchain4j/langchain4j-mcp/pom.properties");
        String javaSdkVersion = artifactVersion(
                "META-INF/maven/io.modelcontextprotocol.sdk/mcp-core/pom.properties");
        return new ProjectMcpStack(
                langChain4jVersion,
                modernClientApiAvailable() ? MODERN_PROTOCOL : ProtocolVersions.MCP_2025_11_25,
                javaSdkVersion,
                latestJavaSdkProtocol());
    }

    /** 1.19.0 起的现代 Client Builder 提供 server/discover 探测超时配置。 */
    public static boolean modernClientApiAvailable() {
        return Stream.of(DefaultMcpClient.Builder.class.getMethods())
                .anyMatch(method -> "protocolDetectionTimeout".equals(method.getName()));
    }

    public static List<FeatureSupport> features() {
        return List.of(
                new FeatureSupport("initialize 会话握手", Support.SUPPORTED,
                        "当前 Client 与 Server 都使用 2025-11-25 旧线握手"),
                new FeatureSupport("stdio Tools", Support.SUPPORTED,
                        "已有真实子进程 listTools/executeTool 回归"),
                new FeatureSupport("Roots、Logging、取消通知", Support.PARTIAL,
                        "存在旧线 API；2026-07-28 已改变交互方式或进入弃用期"),
                new FeatureSupport("server/discover 与逐请求能力", Support.UNSUPPORTED,
                        "当前依赖没有现代无会话握手 API"),
                new FeatureSupport("MRTR input_required", Support.UNSUPPORTED,
                        "当前依赖没有多轮请求结果类型"),
                new FeatureSupport("ttlMs/cacheScope 与 Header 路由", Support.UNSUPPORTED,
                        "当前只具备旧线客户端缓存，不具备 2026 缓存提示和路由头"),
                new FeatureSupport("Tasks 扩展", Support.UNSUPPORTED,
                        "当前依赖没有 tasks/get、tasks/update、tasks/cancel API"),
                new FeatureSupport("远程 OAuth", Support.PARTIAL,
                        "项目实现凭证 audience/scope/日志边界，但未联调完整授权服务器流程"));
    }

    public enum Support {
        SUPPORTED,
        PARTIAL,
        UNSUPPORTED
    }

    /**
     * @param langChain4jMcpVersion 实际使用的 LangChain4j MCP 模块版本
     * @param langChain4jProtocol 该 Client 当前采用的协议版本
     * @param officialJavaSdkVersion 官方 Java MCP SDK 版本
     * @param officialJavaSdkProtocol 该 SDK 当前实现的最新协议版本
     */
    public record ProjectMcpStack(
            String langChain4jMcpVersion,
            String langChain4jProtocol,
            String officialJavaSdkVersion,
            String officialJavaSdkProtocol
    ) {
        public boolean supportsModernProtocolEndToEnd() {
            return MODERN_PROTOCOL.equals(langChain4jProtocol)
                    && MODERN_PROTOCOL.equals(officialJavaSdkProtocol);
        }
    }

    public record FeatureSupport(String feature, Support support, String evidence) {
    }

    private static String latestJavaSdkProtocol() {
        return Stream.of(ProtocolVersions.class.getFields())
                .filter(field -> field.getType() == String.class)
                .map(McpCompatibilityMatrix::readStaticString)
                // MCP 版本采用 ISO 日期，字符串自然顺序就是发布时间顺序。
                .max(String::compareTo)
                .orElseThrow(() -> new IllegalStateException("Java MCP SDK 未声明协议版本"));
    }

    private static String readStaticString(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("无法读取 Java MCP SDK 协议版本", e);
        }
    }

    private static String artifactVersion(String resourcePath) {
        try (InputStream input = McpCompatibilityMatrix.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("依赖缺少版本元数据：" + resourcePath);
            }
            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty("version");
        } catch (IOException e) {
            throw new IllegalStateException("无法读取依赖版本元数据：" + resourcePath, e);
        }
    }
}
