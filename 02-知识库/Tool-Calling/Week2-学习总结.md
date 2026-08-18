# Week 2 学习总结：Tool Calling —— 真正进入 Agent

> 日期：2026-08-16 ~ 2026-08-18 ｜ 周总结：`01-每周学习/Week-02-Tool-Calling/周总结.md`

## 一、本周主线

从「LLM 只会回答问题」到「LLM 能调用你的 Java 代码，完成真实业务操作」。交付物是一个「企业订单 Agent」：能查订单、查用户、查商品、查物流、修改订单状态，全部通过自然语言驱动。

![Week 2 学习路线图](images/Week2-学习路线图.png)

```mermaid
flowchart LR
    D1["Day 1 原理"] --> D2["Day 2 第一个 Tool"]
    D2 --> D3["Day 3 查询接口"]
    D3 --> D4["Day 4 多工具"]
    D4 --> D5["Day 5 修改状态"]
    D5 --> D6["Day 6 防循环与兜底"]
    D6 --> D7["Day 7 周总结"]
```

## 二、七天内容回顾

| 天 | 主题 | 交付物 | 关键点 |
| --- | --- | --- | --- |
| Day 1 | Function Calling 原理 | 概念笔记 | Tool 三要素（name/description/parameters）、Agent Loop、何时用 Tool |
| Day 2 | 第一个 Java Tool | `OrderTools.getOrder` + AiServices 注册 | @Tool/@P 注解、代理自动生成实现、`AiServices.tools()` |
| Day 3 | 企业订单 Agent 查询接口 | `POST /agent/order` | 控制器 + 异常处理，自然语言 → 工具 → 回复 |
| Day 4 | 扩展多工具 | getUser / getProduct / getLogistics | 4 个工具并存，描述质量决定模型选择 |
| Day 5 | 修改订单状态 | `updateOrderStatus` | 三重防线（枚举校验/存在性/仅 PENDING）、安全边界在代码 |
| Day 6 | 错误处理与防循环 | `maxSequentialToolsInvocations` + AgentProperties | 工具异常兜底、防死循环上限 |
| Day 7 | 周总结 | 本文件 | 知识归纳 |

## 三、必须掌握的知识点（按掌握程度）

1. **Agent Loop 机制**：模型不执行代码，它输出「调哪个工具 + 什么参数」的结构化意图；框架执行你的方法，结果回填，模型继续。这是 Tool Calling 的心脏。
2. **@Tool / @P 注解**：`@Tool("描述")` 告诉模型什么时候用；`@P("参数说明")` 告诉模型怎么填参。自动生成 ToolSpecification。
3. **AiServices 动态代理**：`AiServices.builder(接口.class).chatModel(...).tools(...).build()` 在运行时生成接口实现，自动处理 Agent Loop。接口方法名可任意，看注解不看名字。
4. **工具描述质量**：描述模糊 → 模型选错工具。描述要含入参示例（如"订单号，例如 O1001"）。
5. **安全边界在代码**：模型只是发起方，真正的校验/权限在 Java 方法里（仅 PENDING 可改、枚举校验）。
6. **防死循环**：`maxSequentialToolsInvocations(3)` 限制一轮内连续工具调用，防无限烧钱。
7. **工具异常兜底**：工具抛异常时 Agent 不崩溃，AiServices 把异常回填给模型由其兜底回答。
8. **配置类新写法**：record + `@ConfigurationProperties` + compact constructor（默认值/校验）。

## 四、本周常用语法速查

```java
// 1. 定义工具
@Tool("根据订单号查询订单信息")
public String getOrder(@P("订单号，例如 O1001") String orderId) { ... }

// 2. 定义 Agent 接口
public interface OrderAssistant {
    @SystemMessage("你是企业订单助手……")
    String chat(@UserMessage String message);
}

// 3. 装配（含防循环上限）
OrderAssistant assistant = AiServices.builder(OrderAssistant.class)
        .chatModel(chatModel)
        .tools(orderTools)
        .maxSequentialToolsInvocations(3)
        .build();

// 4. 配置类（record 写法）
@ConfigurationProperties(prefix = "agent")
public record AgentProperties(Integer maxSequentialToolsInvocations) {
    public AgentProperties {
        if (maxSequentialToolsInvocations == null || maxSequentialToolsInvocations < 1) {
            maxSequentialToolsInvocations = 3;
        }
    }
}
```

## 五、验证与测试情况

- `mvn test` 全量通过：OrderToolsTest 9 个（工具行为与规则）、AgentPropertiesTest 3 个（配置默认值）、控制器测试 2 个。
- 真实 DeepSeek 联调 `OrderAgentLiveTest` 5 个：查订单 / 查用户 / 查物流 / 改状态 / **工具抛异常兜底** 全部通过。
- 证据链：查订单回复出现「399」、查用户出现「张三」、查物流出现「顺丰」——都只可能来自 Java 工具返回值，证明模型真的调用了代码。

## 六、与面试问题的对应

| 面试问题 | 本周答案 |
| --- | --- |
| Tool 描述写得不好会怎样？ | 模型选错工具或不敢选（Day 4） |
| Agent 陷入循环怎么办？ | `maxSequentialToolsInvocations` 硬上限（Day 6） |
| Tool 的入参由模型生成，如何保证安全？ | 工具内参数校验 + 业务规则（Day 5） |
| 为什么 Tool 权限要最小化？ | 模型可能被诱导调用危险工具，安全边界在代码（Day 5/Week 5 展开） |

## 七、下周预告（Week 3：RAG）

Agent 目前只认识内存里的订单数据。Week 3 解决「模型不知道公司内部知识」的问题：Embedding、向量检索、RAG 问答，让 Agent 基于真实文档回答并给出引用。