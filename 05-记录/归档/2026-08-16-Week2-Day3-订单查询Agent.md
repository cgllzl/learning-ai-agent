# Week 2 Day 3 归档：企业订单 Agent 查询接口（2026-08-16）

> 学习计划：`01-每周学习/Week-02-Tool-Calling/学习目标.md` ｜ 笔记：`02-知识库/Tool-Calling/Day3-订单查询Agent.md`

## 今天做了什么

1. **`OrderAgentRequest`**：请求 DTO（message @NotBlank）。
2. **`OrderAgentController`**：`POST /agent/order` —— 自然语言 → Agent → 工具 → 回复。
3. **`OrderAgentExceptionHandler`**：400 / 503 / 502 统一错误码。
4. **`OrderAgentControllerTest`**：接口返回、空参数 400。
5. 文档：requests.http 示例、README 接口说明、Sprint-02、学习目标 Day 3 勾选。

## 验证结果

- `mvn test`：全部通过（新增 OrderAgentControllerTest 2 个）
- 真实 DeepSeek 联调（Day 2 已建 `OrderAgentLiveTest`）：查订单回复含「399」——证明工具真实调用

## 完成标准（Day 3）

- [x] 实现「企业订单 Agent」：查询订单（HTTP 接口可用）
- [x] 自然语言提问能触发正确 Tool

## 下一步（Day 4）

- 扩展：查询用户 / 物流 / 商品（加工具 + 更新 MockOrderData）