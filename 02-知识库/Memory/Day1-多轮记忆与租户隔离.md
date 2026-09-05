# Day 1：多轮记忆与租户隔离（补全 Memory 模块）

> 补全日期：2026-09-05 ｜ 代码：`com.enterprise.agent.memory`

## 一、先讲人话：为什么 Agent 需要记忆

大模型本身是无状态的：你不把上一轮的话再发一遍，它就不知道你们聊过什么。要让 Agent 能“记住”，就要把历史消息保存下来，并在下一轮请求时拼回去。

企业里更关键的是：**记忆必须按租户隔离**。同一个客服系统服务多家公司，A 公司的对话上下文绝对不能带进 B 公司的回答里。

## 二、核心实现

### 1. 用 MessageWindowChatMemory 保存最近 N 条消息

```java
ChatMemory memory = MessageWindowChatMemory.builder()
        .id(tenantId)
        .maxMessages(8)
        .build();
```

解释：`maxMessages(8)` 表示只保留最近 8 条消息，超过就丢掉最旧的，避免上下文无限增长。

### 2. 每轮请求拼历史、写回记忆

```java
List<ChatMessage> messages = new ArrayList<>();
messages.add(SystemMessage.from(SYSTEM_PROMPT));
messages.addAll(memory.messages());       // 带上历史
messages.add(UserMessage.from(userMessage));

ChatResponse response = chatModel.chat(messages);
memory.add(UserMessage.from(userMessage), AiMessage.from(reply));
```

### 3. 按租户分仓库

```java
private final Map<String, ChatMemory> memories = new ConcurrentHashMap<>();

public ChatMemory forTenant(String tenantId) {
    return memories.computeIfAbsent(tenantId, id ->
            MessageWindowChatMemory.builder().id(id).maxMessages(8).build());
}
```

`ConcurrentHashMap` 保证线程安全，`computeIfAbsent` 让同一租户总是拿到同一段记忆。

## 三、学习例子 + 企业例子

`MemoryLiveTest` 用真实 DeepSeek 验证：

- 学习例子：t1 第一轮说“订单号 O1001”，第二轮问“我的订单号是多少？”→ 回答 O1001。
- 企业例子：t2 说“订单号 O2001”，第二轮问同样的问题 → 回答 O2001，不会串到 O1001。

这直接对应多租户 SaaS 客服系统：**每个企业都有自己的记忆抽屉，互不干扰。**

## 四、如何本地测试

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent

# 1) 不调大模型，验证租户记忆隔离
mvn test -Dtest=TenantMemoryStoreTest

# 2) 真实 DeepSeek 联调：多轮记忆 + 多租户隔离
.\scripts\test-live.ps1 -Test MemoryLiveTest
```

## 五、完成标准

- [x] Memory 目录不再只有 README
- [x] 多轮记忆可用
- [x] 记忆按租户隔离
- [x] 有真实调用大模型的学习例子和企业例子
