# Week 5 Day 5 归档：Secret 管理（2026-08-31）

> 学习计划：`01-每周学习/Week-05-企业级Agent安全/学习目标.md` ｜ 笔记：`02-知识库/Agent安全/Day5-Secret管理.md`

## 今天做了什么

1. 实现 `SecretMasker`：长密钥保留头尾各 4 位，短密钥全打码。
2. 实现 `SecretValue`：密钥包装对象，`raw()` 取原文，`toString()` 永远返回脱敏值。
3. 实现 `SecretSafeChatService`：内部用原文建模型，外部配置摘要只用脱敏值。
4. 用真实 DeepSeek 验证：脱敏不影响调用，摘要不泄露密钥。

## 验证

- `SecretMaskerTest`：长密钥正确脱敏、短密钥全打码。
- `SecretValueTest`：toString 不泄露原文。
- `SecretManagementLiveTest`（真实 DeepSeek）：摘要显示 `sk-4****8659`，正常对话成功。

## 完成标准（Day 5）

- [x] 密钥对象 toString 不泄露明文
- [x] 配置摘要使用脱敏后的密钥
- [x] 有真实调用大模型的例子（`SecretManagementLiveTest`）

## 下一步（Day 6）

- 审计日志 + 高危操作人工审批流
