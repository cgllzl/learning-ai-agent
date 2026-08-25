# Day 5：Supervisor 模式 —— 让主 Agent 分派任务

> Week 4 ｜ 归档：`05-记录/归档/2026-08-25-Week4-Day5-Supervisor.md` ｜ 代码：`com.enterprise.agent.supervisor`

## 一、一个贴切的类比：前台总机

把整个系统想象成一家公司的客服热线：

- 你打电话进来，先接电话的是**前台总机**（Supervisor）。
- 总机自己不解决具体问题，它只做一件事：听清楚你要什么，然后**转接**。
- 「我要查订单」→ 转订单部；「我想问公司年假政策」→ 转 HR 知识库。

这就是 Supervisor 模式：**一个主 Agent 负责"分诊"，多个子 Agent 负责"看病"。**

## 二、本项目怎么实现

我们手上已经有两个现成的"部门"：

- **订单子助手**（Week 2 的 `OrderAgentService`）：会查订单、用户、物流、改状态。
- **知识子助手**（Week 3 的 `RagQaService`）：会检索企业文档回答问题。

现在只需要一个总机把它们接起来。

### 第 1 步：把每个子助手包装成一个工具

```java
@Component
public class SupervisorTools {

    @Tool("处理订单、用户、商品、物流、修改订单状态等业务问题")
    public String handleOrder(@P("用户的业务问题") String question) {
        return orderAgentService.chat(question);   // 转给订单子助手
    }

    @Tool("处理企业内部知识库、制度文档类问题")
    public String handleKnowledge(@P("用户的知识问题") String question) {
        return ragQaService.ask(question, null, 5).answer();   // 转给知识子助手
    }
}
```

关键点：**子助手本身也是一个 Agent**，这里只是把它当成一个"工具"暴露给上级。子助手内部该调工具调工具、该跑 RAG 跑 RAG，上层完全不用管——这就是嵌套 Agent。

### 第 2 步：给总机写"分诊规则"

```java
public interface SupervisorAssistant {

    @SystemMessage("""
            你是企业 AI 助手的总调度员。
            先判断用户问题属于哪一类：
            - 订单、用户、商品、物流、修改状态 → 调用 handleOrder
            - 公司制度、文档、知识问答 → 调用 handleKnowledge
            然后基于子助手返回的结果，用中文简洁地回答用户。""")
    String chat(@UserMessage String message);
}
```

`@SystemMessage` 就是总机的"值班守则"——告诉它遇到什么情况拨哪个分机。

### 第 3 步：组装 Supervisor

```java
SupervisorAssistant assistant = AiServices.builder(SupervisorAssistant.class)
        .chatModel(chatModel)
        .tools(supervisorTools)              // 两个"分机"按钮
        .maxSequentialToolsInvocations(3)    // 防止反复转接死循环
        .build();
```

## 三、为什么要这样分层

- **单一职责**：订单 Agent 只管订单，知识 Agent 只管知识，总机只管路由。每个都简单、好测试、好替换。
- **可扩展**：以后加一个"数据分析子助手"，只要在 `SupervisorTools` 里加一个 `@Tool` 方法，总机自动多一个分机按钮，不用动其它代码。
- **隔离**：某个子助手出问题，只影响那一类问题，不会拖垮整个系统。

## 四、验证

- `SupervisorToolsTest`（2 个）：`handleOrder` 正确委托订单助手、`handleKnowledge` 正确委托知识助手。
- `SupervisorLiveTest`（真实 DeepSeek）：问「查询订单 O1001」，总机自动转订单部，回复带出 O1001 和 399——证明分派成功。

## 五、Day 5 完成标准

- [x] 实现 Supervisor 模式：主 Agent 分派任务