# Sprint 8：Evaluation

## 目标
让 Agent 的行为可评估、可回归。

## 任务
- [x] 建立核心场景评估用例集
- [x] 自动化评估脚本（正确性 / 引用准确性 / 权限）
- [x] 评估结果接入 CI，回归自动跑
- [x] 针对失败用例复盘并改进
- [x] 建立 AgentTrajectory，记录模型、Tool、参数摘要、审批、Handoff、Token、成本与耗时
- [x] 使用三层硬门禁分别评估最终答案、业务结果和执行轨迹
- [x] 轨迹规则覆盖必需/禁止 Tool、关键参数和审批顺序
- [x] 建立开发集与独立留出集
- [x] 相同用例重复运行 5 次，统计成功率、方差、平均成本、P95 延迟和平均步骤
- [x] Single-Agent 与 Multi-Agent 使用相同输入、模型、数据和验收标准进行比较
- [x] 轨迹评估加入离线 CI；真实模型重复实验独立运行

## 技术
Evaluation / Trajectory / Holdout / Repeated Runs / P95 / Cost / Pareto / CI

## 知识库映射
- `02-知识库/Evaluation/`

## 完成标准
- [x] 每次代码变更 CI 自动跑评估
- [x] 关键指标有基线
- [x] 答案正确但行为违规时仍会失败
- [x] CI 可以离线回归轨迹与架构比较逻辑
- [x] 能用真实运行数据回答 Single-Agent 与 Multi-Agent 的选择
