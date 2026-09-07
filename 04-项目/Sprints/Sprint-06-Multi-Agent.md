# Sprint 6：Multi-Agent

## 目标
用 Supervisor 和受代码约束的 Workflow 组织多个 Agent 分工协作。

## 任务
- [x] 定义子 Agent（订单 / 知识 / 客服回复）
- [x] Supervisor 分派
- [ ] Handoff（本周未做代码级落地）
- [x] Sequential 流程（订单 Agent → 客服回复 Agent）
- [x] Agent State 传递与结果合并
- [x] Conditional Workflow（Week 6.5 Day 1：订单 / 知识库 / 人工分支）
- [x] 有限 Loop（Week 6.5 Day 1：答复复核 / 修改，超限转人工）
- [x] Agent Run + Checkpoint（Week 6.5 Day 2：按步骤保存并从失败点恢复）
- [x] 幂等与补偿（Week 6.5 Day 2：避免重复副作用，失败后可恢复原订单状态）
- [ ] 评估：Multi-Agent 是否真的比 Single-Agent 好

## 技术
Orchestration / Supervisor / Handoff / State / Conditional / Limited Loop / Checkpoint / Idempotency / Compensation

## 知识库映射
- `02-知识库/Agent编排/`

## 完成标准
- [x] 一个复杂任务由多个 Agent 协作完成
- [x] 企业工单能进入订单、知识库或人工分支
- [x] 质量复核循环达到硬上限后自动停止并转人工
- [x] 订单更新后通知失败，可从通知步骤恢复且不重复修改订单
- [x] 审批等待和 Agent Run 按租户隔离
