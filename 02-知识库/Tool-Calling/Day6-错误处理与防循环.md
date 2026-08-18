# Day 6：Tool 错误处理与防循环

> Week 2 ｜ 归档：`05-记录/归档/2026-08-18-Week2-Day6-错误处理与防循环.md` ｜ 代码：`com.enterprise.agent.agent`

## 一、本日目标：Agent 的三种"翻车"场景

| 场景 | 问题 | 对策 |
| --- | --- | --- |
| 工具报错 | 工具抛异常，Agent 崩溃或回答不了 | AiServices 内置兜底 + 工具内返回友好错误信息 |
| 工具超时 | 工具/模型一直不返回 | 模型层 `deepseek.timeout` 超时；工具层快速失败 |
| 工具选错 | 模型调错工具或填错参数 | 工具描述清晰 + 参数校验 + 最大调用次数上限 |
| 死循环 | 模型反复调工具，停不下来（烧钱） | `maxSequentialToolsInvocations` 硬上限 |

## 二、防死循环：maxSequentialToolsInvocations（新语法）

```java
OrderAssistant assistant = AiServices.builder(OrderAssistant.class)
        .chatModel(chatModel)
        .tools(orderTools)
        .maxSequentialToolsInvocations(3)   // ← 连续调用工具最多 3 次
        .build();
```

- **作用**：Agent Loop 中，模型连续请求调用工具的次数一旦达到上限，框架强制结束循环，把控制权交还给模型直接回答。
- **为什么必须有**：每多一轮工具调用就多一次模型请求（token 成本），模型可能因提示注入或自身失误陷入「查了又查」的循环，不设上限会无限烧钱。
- 设为 3 的含义：一轮查询场景（查订单 → 查用户 → 查商品）足够；上限不是「总调用数」，而是「一轮内连续调用数」。

### 配套：AgentProperties 配置类（新语法：@ConfigurationProperties record）

```java
@ConfigurationProperties(prefix = "agent")
public record AgentProperties(Integer maxSequentialToolsInvocations) {

    public AgentProperties {
        // compact constructor：参数绑定后在这里做默认值/校验
        if (maxSequentialToolsInvocations == null || maxSequentialToolsInvocations < 1) {
            maxSequentialToolsInvocations = 3;
        }
    }
}
```

```yaml
# application.yml
agent:
  max-sequential-tools-invocations: 3
```

三个新知识点：
1. **record + @ConfigurationProperties**：Java 21 record 天然适合配置类，构造参数即绑定字段（Spring Boot 3 支持构造绑定）。
2. **compact constructor**：record 里不写普通构造器，而是写「紧凑构造器」`public AgentProperties { ... }`，参数自动赋值，花括号里做默认值/校验。
3. **注册方式**：在某个 `@Configuration` 上 `@EnableConfigurationProperties({DeepSeekProperties.class, AgentProperties.class})` 批量注册；或全项目 `@ConfigurationPropertiesScan`。

## 三、工具报错兜底：AiServices 的内置行为（重点）

**LangChain4j 默认行为**：工具方法抛异常时，Agent 不会崩溃——框架捕获异常，把错误信息作为一条 Tool 消息回填给模型，模型据此给出"失败/抱歉"之类的回答。

实测（`OrderAgentLiveTest.agentSurvivesToolFailure`）：

```java
// 故意抛异常的工具
public static class FlakyTools {
    @Tool("无论调用什么都会抛异常的测试工具")
    public String alwaysFail(@P("任意入参") String anything) {
        throw new IllegalStateException("模拟工具崩溃");
    }
}
```

用户问「调用 alwaysFail 工具看看会发生什么」→ 工具抛异常 → Agent 没有崩溃，回复类似：
「调用 alwaysFail 工具的结果：**工具执行失败**（模拟工具崩溃）…」

**最佳实践**：
- 普通校验失败（参数非法、查无数据）→ 工具返回**错误描述字符串**（如 `"未找到订单 O1001"`），模型能自然接话。
- 真正的系统异常 → 让框架兜底（抛异常回填给模型），或包一层 try/catch 转成友好消息。
- 不要为了"显得健壮"把异常吞成空字符串——模型会困惑。

## 四、工具超时

- **模型层**：`deepseek.timeout: 30s`（Day 5 配置）——单次 HTTP 请求超时，由 `OpenAiChatModel` 负责。
- **工具层**：本项目的工具是内存查询，毫秒级；生产环境工具若查数据库/外部服务，要在工具内部做超时（如 JDK 虚拟线程 + `orTimeout`，或数据库连接超时），避免模型等一个永远不返回的工具。
- 记住分层：模型超时 ≠ 工具超时，两层都要管。

## 五、工具选择错误兜底（三层配合）

1. **描述质量**（Day 4）：`@Tool` 描述 + `@P` 参数示例写清楚，模型少选错。
2. **参数校验**（Day 5）：工具内部校验入参，错了返回友好错误。
3. **最大调用次数**（本日）：即使模型反复选错，也会在上限处停下并转为回答。

三者缺一不可：描述差→经常选错；不校验→错误参数进业务；没上限→选错也停不下来。

## 六、验证

- `AgentPropertiesTest`（3 个）：默认值 3 / 非法值回退 3 / 自定义值生效。
- `OrderAgentLiveTest`（5 个，真实 DeepSeek）：查订单/查用户/查物流/改状态 + **工具抛异常兜底**全部通过。
- `mvn test` 全量通过。

## 七、面试问题（呼应 Day 1）

- Agent 陷入循环怎么办？→ `maxSequentialToolsInvocations` 硬上限 + 工具尽量幂等 + 监控 token 用量。
- 为什么 Tool 权限要最小化？→ 模型可能被诱导调用危险工具，见 Day 5「安全边界在代码」。