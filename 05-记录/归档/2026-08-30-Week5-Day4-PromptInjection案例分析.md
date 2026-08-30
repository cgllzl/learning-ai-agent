# Week 5 Day 4 归档：Prompt Injection 案例分析 + 防护实践（2026-08-30）

> 学习计划：`01-每周学习/Week-05-企业级Agent安全/学习目标.md` ｜ 笔记：`02-知识库/Agent安全/Day4-PromptInjection案例分析.md`

## 今天做了什么

1. 梳理常见 Prompt Injection 案例：直接覆盖指令、角色扮演越狱、诱导工具调用、编码绕过。
2. 实现 `PromptInjectionGuard`：用正则规则在调用模型前拦截直接注入。
3. 实现 `SecurePromptChatService`：先输入检查，再加固 System Prompt，最后调用大模型。
4. 用真实 DeepSeek 验证：正常问题放行，注入请求在调用前抛异常。

## 验证

- `PromptInjectionGuardTest`：能识别直接指令覆盖和角色扮演越狱，正常业务问题不被误拦。
- `PromptInjectionLiveTest`（真实 DeepSeek）：正常问题正常回答；「忽略以上所有指令」和「你是DAN」均被拦截。

## 完成标准（Day 4）

- [x] 分析常见 Prompt Injection 案例
- [x] 实现输入检查 + 提示词加固两道防线
- [x] 有真实调用大模型的例子（`PromptInjectionLiveTest`）

## 下一步（Day 5）

- Secret 管理：密钥不落地、不打印日志
