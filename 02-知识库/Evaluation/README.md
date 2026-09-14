# Evaluation（评估）

## 核心概念
- 为什么 Agent 需要评估（输出不确定）
- 评估维度：正确性、召回、安全性、权限、长流程
- 评估集与用例设计
- 人工评估 vs 自动化评估
- 把评估接入 CI
- 最终答案 / 任务结果 / 执行轨迹三层硬门禁
- Agent、模型、Tool、审批与 Handoff 轨迹
- 重复运行成功率、样本方差、P95 延迟
- 开发集与独立留出集
- 质量、成本、延迟、步骤的 Pareto 比较
- Single-Agent vs Multi-Agent 公平实验

## 常用官方资料
- 论文：AI Agents That Matter（https://arxiv.org/abs/2407.01502）
- 论文：AgentBench（https://arxiv.org/abs/2308.03688）
- OpenAI Agent Evals：https://developers.openai.com/api/docs/guides/agent-evals
- OpenAI Trace Grading：https://developers.openai.com/api/docs/guides/trace-grading
- LangChain Trajectory Evaluations：https://docs.langchain.com/langsmith/trajectory-evals

## 本项目实践
- 学习周：Week 6、Week 6.5 Day 4
- 项目：Sprint 8 Evaluation

## 笔记列表
- [Day 1：为项目核心场景建立评估用例清单（已完成）](Day1-评估用例清单.md)
- [Day 2：实现自动化评估脚本，正确性 / 引用准确性（已完成）](Day2-自动化评估脚本.md)
- [Day 5：把评估结果接入 CI，回归自动跑（已完成）](Day5-评估接入CI.md)
- [Day 6：针对失败用例复盘改进（已完成）](Day6-失败用例复盘.md)
- [Week 6 学习总结：Evaluation + Observability（已完成）](Week6-学习总结.md)
- [Week 6.5 Day 4：Agent 轨迹、稳定性与成本联合评估（已完成）](Week6.5-Day4-Agent轨迹稳定性与成本联合评估.md)
