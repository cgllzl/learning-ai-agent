# Day 5：Secret 管理 —— 密钥不落地、不打印日志

> Week 5 ｜ 归档：`05-记录/归档/2026-08-31-Week5-Day5-Secret管理.md` ｜ 代码：`com.enterprise.agent.security.SecretValue`

## 一、先讲人话：为什么密钥这么容易漏

API Key、数据库密码这类东西，本质上是「系统的家门钥匙」。管理密钥最怕两件事：

1. **密钥落地**：把明文写进代码仓库、配置文件、聊天记录，等于把钥匙复制了很多份。
2. **密钥进日志**：某个异常或调试语句不小心把对象 `toString()` 出来，钥匙就跟着日志一起被打印了。

Day 5 的目标不是发明密码学，而是养成两个工程习惯：

- 密钥只在环境变量或密钥管理服务里，代码仓库里永远没有明文。
- 密钥对象即使被日志打印，也不会泄露明文。

## 二、当前项目已经做对的一半

`application.yml` 里已经做到「不硬编码」：

```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY:}
```

这行配置的意思是：从环境变量 `DEEPSEEK_API_KEY` 读取；如果没设置，就填空。它保证了密钥不在配置文件里落地。

但还有另一半没做：`DeepSeekProperties.apiKey` 目前是普通 `String`，如果不小心 `System.out.println(props)`，密钥就出来了。Day 5 补上这一半。

## 三、用 SecretValue 把密钥包起来

```java
public final class SecretValue {

    private final String raw;

    private SecretValue(String raw) {
        this.raw = raw;
    }

    public static SecretValue of(String raw) {
        return new SecretValue(raw);
    }

    public String raw() {
        return raw;
    }

    public String masked() {
        return new SecretMasker().mask(raw);
    }

    @Override
    public String toString() {
        return masked();
    }
}
```

这个类的关键设计是：

- 真正要用密钥时，明确调用 `raw()`；
- 但 `toString()` 永远返回 `masked()`，也就是脱敏后的值。

这样即使有人写了 `log.info("config={}", secretValue)`，打印出来的也是 `sk-1****ghij`，而不是完整密钥。

## 四、SecretMasker：怎么脱敏

```java
public class SecretMasker {

    public String mask(String secret) {
        if (secret == null) {
            return null;
        }
        if (secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}
```

解释：

- 长度小于等于 8 的密钥直接全部打码，因为太短了，保留任何一部分都还有风险。
- 长密钥只保留头 4 位和尾 4 位，中间用 `****` 替代，方便运维看到「这是哪个 Key」但看不到完整值。

## 五、把它接进一个安全聊天服务

```java
public class SecretSafeChatService {

    private final SecretValue apiKey;
    private final OpenAiChatModel chatModel;

    public SecretSafeChatService(String apiKey) {
        this.apiKey = SecretValue.of(apiKey);
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();
    }

    public String chat(String systemPrompt, String userMessage) {
        ChatResponse response = chatModel.chat(List.of(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userMessage)));
        return response.aiMessage().text();
    }

    public String safeConfigSummary() {
        return "api-key=" + apiKey.masked();
    }
}
```

注意：`OpenAiChatModel` 内部确实需要真实密钥，所以这里调用 `apiKey(apiKey)` 传入原文；但对外展示配置时用的是 `apiKey.masked()`。这就是「内部能取用、外部不泄露」。

## 六、大模型例子：脱敏不影响调用，密钥不出现在摘要里

`SecretManagementLiveTest` 用真实 DeepSeek 验证三件事：

```java
String apiKey = System.getenv("DEEPSEEK_API_KEY");
SecretSafeChatService service = new SecretSafeChatService(apiKey);

// 1) 配置摘要只包含脱敏后的密钥
String summary = service.safeConfigSummary();
assertThat(summary).contains("****");
assertThat(summary).doesNotContain(apiKey);

// 2) 密钥包装对象的 toString 也不泄露明文
assertThat(SecretValue.of(apiKey).toString()).doesNotContain(apiKey);

// 3) 真实调用 DeepSeek，证明脱敏不影响正常使用
String reply = service.chat("你是一个友好的助手", "请用一句话介绍你自己");
```

真实输出：

```text
[安全配置摘要] api-key=sk-4****8659
[Secret 管理回答] 你好！我是一个乐于助人的AI助手...
```

从这个输出能看出：密钥被脱敏了，但模型调用正常。

## 七、如何本地测试

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent

# 1) 不调大模型，验证脱敏逻辑
mvn test -Dtest=SecretMaskerTest,SecretValueTest

# 2) 真实 DeepSeek 联调：脱敏 + 正常调用
.\scripts\test-live.ps1 -Test SecretManagementLiveTest
```

## 八、Day 5 完成标准

- [x] 密钥对象 toString 不泄露明文
- [x] 配置摘要使用脱敏后的密钥
- [x] 有真实调用大模型的例子（`SecretManagementLiveTest`）
