# Tool Calling

## 核心概念（Week 2 学习路线）
- Function Calling 原理（Day 1 ✅）
- Tool 定义：name / description / parameters / JSON Schema（Day 2）
- Agent Loop：决策 → 调用 → 结果回填 → 继续（Day 3）
- LangChain4j @Tool 注解与 AiServices（Day 2/3）
- Tool 参数校验与错误处理（Day 5/6）
- 最大迭代次数与防循环（Day 6）
- Tool 权限（联动 Week 5 Agent 安全）

## 每日笔记（Day 1~7 全部完成 ✅）
- [Day 1：Function Calling 原理](Day1-Function-Calling原理.md)
- [Day 2：第一个 Java Tool](Day2-第一个Java-Tool.md)
- [Day 3：企业订单 Agent 查询接口](Day3-订单查询Agent.md)
- [Day 4：扩展查询用户/物流/商品](Day4-查询用户物流商品.md)
- [Day 5：修改订单状态与权限](Day5-修改订单状态与权限.md)
- [Day 6：错误处理与防循环](Day6-错误处理与防循环.md)
- [Week 2 学习总结（路线图 + 语法速查）](Week2-学习总结.md)
- [FAQ：常见疑问](FAQ-常见疑问.md)

## 常用官方资料
- LangChain4j Tools：https://docs.langchain4j.dev/tutorials/tools
- OpenAI Function Calling：https://platform.openai.com/docs/guides/function-calling

## 本项目实践
- 学习周：Week 2（已完成，周总结见 `01-每周学习/Week-02-Tool-Calling/周总结.md`）
- 项目：Sprint 2 Tool Calling 全部完成（企业订单 Agent：查订单/用户/商品/物流 + 改状态）