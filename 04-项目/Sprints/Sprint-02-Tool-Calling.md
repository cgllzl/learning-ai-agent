# Sprint 2：Tool Calling

## 目标
让 Agent 能调用业务代码，完成「企业订单 Agent」。

## 任务
- [ ] 用 @Tool 定义：查订单 / 查用户 / 查物流 / 查商品
- [ ] 修改订单状态（带参数校验 + 权限校验）
- [ ] Agent 自动选择工具，结果回填继续对话
- [ ] 最大迭代次数限制，防循环
- [ ] Tool 调用错误处理与兜底话术

## 技术
Function Calling / @Tool / Agent Loop / JSON Schema

## 知识库映射
- `02-知识库/Tool-Calling/`

## 完成标准
- [ ] 自然语言提问能触发正确 Tool
- [ ] 越权修改订单被拒绝
