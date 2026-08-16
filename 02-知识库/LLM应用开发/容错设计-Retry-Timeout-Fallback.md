# 容错设计：Retry / Timeout / Fallback（Day 5）

> 归档：`05-记录/归档/2026-08-16-Day5-容错设计.md` ｜ 项目：`04-项目/enterprise-agent`

## 一、为什么 LLM 应用需要容错

- 第三方模型 API 不稳定：网络抖动、超时、限流（429）、服务端 5xx、密钥过期。
- 一次「对用户可见的失败」成本高（体验差、业务流程中断），所以调用前要做好重试、超时与降级。

## 二、三个概念

| 概念 | 作用 | 典型触发 |
| --- | --- | --- |
| Timeout 超时 | 单次请求最长时间，防止一直卡住 | 网络慢、模型响应慢 |
| Retry 重试 | 对「暂时性失败」自动重试，配合退避避免加重负载 | 5xx、429、网络/连接错误 |
| Fallback 降级 | 重试仍失败时切备用模型/备用方案 | 主模型持续不可用 |

## 三、什么错误该重试（关键判断）

LangChain4j 1.18 的异常体系已经帮我们分好类：

- `RetriableException` → 可重试（`RateLimitException` 429、`InternalServerException` 5xx）
- `NonRetriableException` → 不可重试（`InvalidRequestException` 400、`AuthenticationException` 401、模型不存在等）
- `HttpException(statusCode)` → 通用 HTTP 错误，按状态码判断（>=500 或 429 可重试）
- `IOException` → 网络类错误，可重试

**设计要点（踩坑）**：不可重试错误（参数/认证）绝不能重试或降级，否则会把错误吞掉、返回空结果。实现里只有 `isRetryable(e)` 为真才重试/降级，其余直接抛出。

## 四、本项目实现（ResilientCaller）

```java
public <T> T callWithFallback(Function<OpenAiChatModel, T> modelAction) {
    try {
        return retry(() -> modelAction.apply(primaryModel));   // 主模型 + 指数退避重试
    } catch (RuntimeException primaryFailure) {
        if (!isRetryable(primaryFailure)) throw primaryFailure; // 不可重试直接抛
        try {
            return retry(() -> modelAction.apply(fallbackModel)); // 备用模型再试
        } catch (RuntimeException fallbackFailure) {
            throw new AiServiceUnavailableException(...);         // 全挂 → 503
        }
    }
}
```

- 重试次数：`deepseek.max-retries`（默认 2），退避：500ms 起、指数翻倍、上限 5s。
- 超时：模型 Bean 的 `.timeout(deepseek.timeout)`（默认 30s）。
- 备用模型：`deepseek.fallback-model`（默认同主模型；生产建议配真实备用模型/供应商）。
- 接入点：`ChatService` 与 `StructuredChatService` 都通过 `ResilientCaller` 调模型，一处实现全局生效。
- 流式（`/chat/stream`）暂不重试（流中无法安全重放），保持 `[ERROR]` SSE 事件机制。

## 五、错误码约定

| 场景 | HTTP |
| --- | --- |
| 参数校验失败 / 未知 schema / 未知 mode / 未知 role | 400 |
| AI 服务不可用（重试 + 降级后仍失败） | 503 `{"error":"主模型与备用模型均调用失败…"}` |
| 其他异常 | 502 |

## 六、测试

- `ResilientCallerTest`（5 个）：首次成功 / 重试后成功 / 不可重试快速失败（不降级）/ 主模型失败降级成功 / 双模型失败抛 503 异常。
- 真实联调：结构化 LiveTest 走 `ResilientCaller` 链路验证。

## 七、下一步

- Week 1 完成，进入周总结；然后 Week 2：Tool Calling（真正的 Agent）。