# Week 2 归档：Tool Calling 企业订单 Agent（2026-08-16）

> 对应：Week 2（Tool Calling）核心完成 ｜ 笔记：`02-知识库/Tool-Calling/企业订单Agent实现.md`

## 今天做了什么

1. **新增 `agent` 模块**（`com.enterprise.agent.agent`）：
   - `MockOrderData`：内存模拟订单/用户/商品/物流数据
   - `OrderTools`：5 个 `@Tool` 工具（查订单/用户/商品/物流、改订单状态，带业务规则）
   - `OrderAssistant`：Agent 接口（`@SystemMessage` + `@UserMessage`）
   - `OrderAgentService`：用 `AiServices.builder(...).chatModel(...).tools(...)` 装配
   - `POST /agent/order` 接口 + 异常处理（400/503/502）
2. **pom 增加 `langchain4j` 主模块依赖**（AiServices/@P/服务注解所在）
3. **测试**：全量 45 个通过（OrderToolsTest 8 + 控制器 2 + 原有 35）；**真实 DeepSeek Tool Calling 联调 3 个通过**——自然语言查订单返回了「机械键盘/399」（只能来自工具返回值，证明 Agent 真的调用了 Java 工具）

## 关键技术点

- Tool Calling 本质 = 结构化输出（Day 4）+ 工具执行 + 结果回填循环（Agent Loop）。
- LangChain4j `AiServices` 自动处理 Agent Loop：扫描接口和 @Tool → 生成 ToolSpecification → 执行 → 回填。
- 业务规则放 Java 代码（仅 PENDING 可改状态、状态枚举校验），模型只负责「发起」，安全边界在代码。
- DeepSeek 支持 OpenAI 兼容的 function calling，LangChain4j 开箱即用。

## 完成情况（Sprint 2）

- [x] 用 @Tool 定义：查订单/用户/物流/商品
- [x] 修改订单状态（参数校验 + 业务规则）
- [x] Agent 自动选工具、结果回填继续对话（真实联调验证）
- [ ] 最大迭代次数限制与防循环（Day 6 完善）
- [ ] Tool 调用错误处理与兜底话术（Day 6 完善）

## 下一步

- Week 2 Day 6：Tool 报错/超时/选错兜底 + 最大迭代限制
- Week 3：RAG