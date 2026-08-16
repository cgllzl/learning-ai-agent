# FAQ：Tool Calling 常见疑问（Day 2/3）

> 来源：学习过程中的提问与解答 ｜ 关联：Day2-第一个Java-Tool.md、Day3-订单查询Agent.md

## Q1：模型是在哪里指定的？

**链路**：`application.yml` → `DeepSeekProperties` → `ChatConfig` 的 Bean → `@Qualifier` 注入 `OrderAgentService` → `AiServices.chatModel(...)`

```yaml
# application.yml
deepseek:
  api-key: ${DEEPSEEK_API_KEY:}
  base-url: https://api.deepseek.com
  model: deepseek-chat      # ← 模型在这里配置
```

```java
// ChatConfig：把 OpenAiChatModel 注册为 Spring Bean
@Bean
OpenAiChatModel openAiChatModel(DeepSeekProperties props) {
    return OpenAiChatModel.builder()
            .baseUrl(props.baseUrl()).apiKey(props.apiKey())
            .modelName(props.model())     // deepseek-chat
            .timeout(props.timeout())
            .build();
}
```

```java
// OrderAgentService：构造注入主模型（有两个 OpenAiChatModel Bean，用 @Qualifier 指定主模型）
public OrderAgentService(@Qualifier("openAiChatModel") OpenAiChatModel chatModel, OrderTools orderTools) { ... }
```

- 注意：Day 5 配置了主模型 + 备用模型两个 `OpenAiChatModel` Bean，所以要用 `@Qualifier("openAiChatModel")` 消歧。
- `OrderAgentLiveTest` 不走 Spring，是测试里手动 `builder()` 构造的模型，配置来源一致。

## Q2：`orderAgentService.chat()` 里的 `chat()` 是大模型的还是 OrderAssistant 的？

**入口是 OrderAssistant 接口的 `chat()`，内部最终调大模型的 `chat()`**——同名但完全不同：

| | `assistant.chat(String)` | `chatModel.chat(ChatRequest)` |
| --- | --- | --- |
| 属于 | OrderAssistant 接口（AiServices 代理实现） | OpenAiChatModel（LangChain4j） |
| 入参 | 自然语言字符串 | 结构化 ChatRequest |
| 行为 | 编排整个 Agent 流程（可调工具） | 单次调用大模型（发 HTTP） |
| 调用次数 | 1 次（入口） | Agent Loop 里可能多次 |

- `orderAgentService.chat()` → `assistant.chat()`（接口方法，代理实现）→ 代理内部多次 `chatModel.chat()`（大模型方法）
- 对应 Day 2 调用时序图：`U→P` 是接口方法，`P→L` 才是大模型方法

## Q3：接口方法必须叫 `chat` 吗？

**不必。AiServices 看的是注解和签名，不是方法名。**

```java
public interface OrderAssistant {
    String ask(@UserMessage String message);   // 改名 ask 完全正常
}
```

必须满足的条件：
1. 方法必须声明在接口里（AiServices 只给接口生成代理实现）
2. 用户输入参数要标 `@UserMessage`（告诉代理用户消息从哪来）
3. 返回值类型受支持（`String` / `Response<AiMessage>` / `TokenStream` 等）
4. 调用处与接口声明一致（普通 Java 规则）

一个接口可以声明多个方法，各自带不同的 `@SystemMessage`，AiServices 会分别生成实现。

**容易混淆的原因**：接口方法名恰好和大模型方法名都叫 `chat`；实际上两者完全无关，代理内部永远是调 `chatModel.chat(ChatRequest)`，与你的接口方法叫什么无关。