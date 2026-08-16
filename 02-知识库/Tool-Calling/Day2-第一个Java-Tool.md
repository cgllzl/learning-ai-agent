# Day 2：第一个 Java Tool（@Tool 注解）

> Week 2 ｜ 代码：`com.enterprise.agent.agent.OrderTools`

## 一、@Tool 注解

```java
@Component
public class OrderTools {

    @Tool("根据订单号查询订单信息")
    public String getOrder(@P("订单号，例如 O1001") String orderId) {
        // 真正的 Java 代码：查内存/数据库
        return "订单 O1001：用户 张三，商品 机械键盘，金额 399.0 元，状态 PAID";
    }
}
```

- `@Tool("描述")`：方法的描述，模型据此判断什么时候调用。
- `@P("参数说明")`：参数描述，模型据此生成参数值。
- 返回值：可以是 String 描述，也可以是对象（框架自动序列化成 JSON 给模型）。

## 二、注册给 LLM（关键一步）

```java
OrderAssistant assistant = AiServices.builder(OrderAssistant.class)
        .chatModel(chatModel)   // DeepSeek（OpenAI 兼容）
        .tools(orderTools)      // 把工具注册给模型
        .build();
```

- `AiServices.tools(...)` 会扫描 `OrderTools` 里的 `@Tool` 方法，自动生成 ToolSpecification（name/description/parameters）。
- 注册后，模型就能在需要时"请求调用"这些方法。

## 三、工具方法三原则

1. **单一职责**：一个方法只做一件事（查订单 / 查用户）。
2. **描述清晰**：描述里写明入参格式（如"订单号，例如 O1001"）。
3. **永不盲信入参**：模型可能给错参数，方法内必须做校验（Day 5 详解）。

## 四、验证

`OrderToolsTest`：直接调用工具方法，断言返回值与业务规则（查得到 / 查不到 / 参数非法）。