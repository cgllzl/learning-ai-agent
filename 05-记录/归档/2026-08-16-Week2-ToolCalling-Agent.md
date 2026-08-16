# Week 2 归档：Tool Calling（2026-08-16，按天记录）

> 学习计划：`01-每周学习/Week-02-Tool-Calling/学习目标.md` ｜ 笔记：`02-知识库/Tool-Calling/`

## Day 1 ✅ Function Calling 原理
- 理解：Tool 定义三要素（name/description/parameters）、Agent Loop、何时该用 Tool
- 笔记：`02-知识库/Tool-Calling/Day1-Function-Calling原理.md`

## Day 2 ✅ 第一个 Java Tool
- 完成：`OrderTools.getOrder`（@Tool + @P）、`AiServices.tools()` 注册给 LLM
- 笔记：`Day2-第一个Java-Tool.md`；测试：`OrderToolsTest`

## Day 3 ✅ 企业订单 Agent（查询订单）
- 完成：`OrderAssistant` + `AiServices` 装配 + `POST /agent/order`
- 真实联调：问「查询订单 O1001」回复出现「机械键盘/399」（证明真调用了 Java 工具）
- 笔记：`Day3-订单查询Agent.md`；测试：`OrderAgentLiveTest`

## Day 4 ✅ 扩展多工具
- 完成：`getUser` / `getProduct` / `getLogistics`（共 4+ 个 Tool）
- 真实联调：查 O1002 物流返回「顺丰 SF123456789」
- 笔记：`Day4-查询用户物流商品.md`

## Day 5 ✅ 修改订单状态（参数校验与权限）
- 完成：`updateOrderStatus`——状态枚举校验 + 订单存在性 + 仅 PENDING 可改
- 思想：安全边界在 Java 代码，模型只负责"发起"
- 笔记：`Day5-修改订单状态与权限.md`；测试：`OrderToolsTest` 4 种规则全覆盖

## Day 6 ⏳ 待做
- Tool 报错 / 超时 / 选择错误兜底 + 最大迭代次数限制（`AiServices.maxSequentialToolsInvocations`）

## Day 7 ⏳ 待做
- Week 2 周总结 + 知识库更新（模板在 `01-每周学习/Week-02-Tool-Calling/周总结.md`）

## 技术栈补充
- pom 新增 `langchain4j` 主模块依赖（AiServices / @P / 服务注解）
- 全量测试 45 个通过；真实 DeepSeek Tool Calling 联调 3 个通过