# Week 2 Day 2 归档：第一个 Java Tool（2026-08-16）

> 学习计划：`01-每周学习/Week-02-Tool-Calling/学习目标.md` ｜ 笔记：`02-知识库/Tool-Calling/Day2-第一个Java-Tool.md`

## 今天做了什么

1. **pom 增加 `langchain4j` 主模块依赖**（AiServices / @P 所在）。
2. **`MockOrderData`**：内存模拟 3 笔订单（O1001/O1002/O1003）。
3. **`OrderTools.getOrder`**：第一个 Java Tool——`@Tool("根据订单号查询订单信息")` + `@P("订单号，例如 O1001")`，查不到返回「未找到订单」。
4. **注册给 LLM**：`OrderAssistant` 接口 + `OrderAgentService` 用 `AiServices.builder().chatModel(...).tools(orderTools).build()` 装配。

## 验证结果

- `mvn test`：全部通过（Day2 新增 `OrderToolsTest` 2 个）
- 真实 DeepSeek 联调（`OrderAgentLiveTest`）：问「查询订单 O1001」，回复出现「O1001 / 399」——证明模型真的调用了注册的 Java 工具

## 完成标准（Day 2）

- [x] 写第一个 Java Tool（查询订单）
- [x] 注册给 LLM（AiServices.tools）
- [x] 模型能按意图自动调用（真实联调验证）

## 下一步（Day 3）

- 实现「企业订单 Agent」的对话接口（HTTP POST），把工具能力暴露给外部