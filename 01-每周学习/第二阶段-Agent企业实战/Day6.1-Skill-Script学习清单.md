# Day 6.1：给企业售后 Skill 加确定性 Script

> 状态：2026-10-22 已完成。承接 Week 6.5 Day 6，作为第二阶段的入门练习。

## 回顾与目标

Day 6 已有 Skill 的触发描述和售后 SOP，却还没有亲手运行过 Skill 自带脚本。今天让模型理解“我想取消 O1003”，再让小脚本像柜台的材料清单一样检查资料是否齐全；真正的身份、权限、审批仍由受信业务服务负责。

## 学习任务

- [x] 在 Skill 的 `scripts/` 中增加纯标准库预检器，输入/输出均为 JSON。
- [x] 学习例子：只读查询的字段齐全，返回下一步为运行时鉴权。
- [x] 企业例子：取消订单缺审批和幂等键，返回缺失字段；退款金额不合法或无专用 Tool 时不自动执行。
- [x] 解释 Python 的 `stdin`、`dict`、`Decimal`、`sys.exit`，以及 Java LiveTest 的 `ProcessBuilder` 调用链。
- [x] 用 5 个离线行为测试覆盖只读、缺审批、退款、未知动作、坏 JSON。
- [x] 真实 DeepSeek 提取售后意图，再把候选 JSON 交给脚本预检。
- [x] Skill 版本升至 `1.1.0`，校验器通过，本地与 GitHub CI 纳入脚本回归。
- [x] 知识库笔记、学习日志和项目映射同步。
- [x] 全量 Java 回归：231 个测试、0 失败、0 错误，37 个 LiveTest 在普通环境按设计跳过。

## 运行与完成标准

在 `04-项目/enterprise-agent` 目录：

```powershell
.\scripts\eval-ci.ps1
.\scripts\test-live.ps1 -Test EnterpriseOrderAfterSalesSkillLiveTest
```

只有脚本明确区分“字段齐全”和“已授权”，真实模型不虚构身份/审批，且以上命令可执行，才算完成。详见 `02-知识库/Agent-Skill/Week6.5-Day6.1-Skill-Script确定性预检.md`。
