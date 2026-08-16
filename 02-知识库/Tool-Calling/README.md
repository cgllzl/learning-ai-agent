# Tool Calling

## 核心概念
- Function Calling 原理
- Tool 定义：name / description / parameters / JSON Schema
- Agent Loop：决策 → 调用 → 结果回填 → 继续
- LangChain4j @Tool 注解与 AiServices
- Tool 参数校验与错误处理
- 最大迭代次数与防循环（Day 6）
- Tool 权限（联动 Week 5 Agent 安全）

## 每日笔记（Week 2）
- [Day 1：Function Calling 原理](Day1-Function-Calling原理.md)
- [Day 2：第一个 Java Tool（@Tool）](Day2-第一个Java-Tool.md)
- [Day 3：企业订单 Agent（查询订单）](Day3-订单查询Agent.md)
- [Day 4：扩展多工具（用户/物流/商品）](Day4-查询用户物流商品.md)
- [Day 5：修改订单状态（参数校验与权限）](Day5-修改订单状态与权限.md)
- [Day 6：错误处理与防循环](Day6-错误处理与防循环.md)（待完成）

## 实现速查
- [企业订单 Agent 实现（代码/接口/测试）](企业订单Agent实现.md)

## 常用官方资料
- LangChain4j Tools：https://docs.langchain4j.dev/tutorials/tools
- OpenAI Function Calling：https://platform.openai.com/docs/guides/function-calling

## 本项目实践
- 学习周：Week 2
- 项目：Sprint 2 Tool Calling（企业订单 Agent，`POST /agent/order`）