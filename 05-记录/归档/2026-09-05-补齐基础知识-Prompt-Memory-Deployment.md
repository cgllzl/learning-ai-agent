# 归档：补齐 Prompt 工程 / Memory / 部署与工程化（2026-09-05）

> 目的：在开始企业级项目实战前，先补齐第一部分基础知识缺口。

## 今天做了什么

1. 补全 Prompt 工程：
   - 新增 `PromptEngineeringService`：System Prompt、Few-shot、Chain-of-Thought、企业工单分诊。
   - `PromptEngineeringLiveTest` 真实 DeepSeek 联调通过。
2. 补全 Memory 模块：
   - 新增 `MemoryDemoService` 与 `TenantMemoryStore`：多轮记忆 + 多租户隔离。
   - `MemoryLiveTest` 真实 DeepSeek 联调通过。
3. 补全部署与工程化：
   - 新增 `Dockerfile` 与 `docker-compose.yml`。
   - 写清多阶段构建、密钥注入、部署后冒烟。
4. 修复勾选一致性：
   - Week1 学习目标 Day1~7 与完成标准勾选。
   - Sprint-00 / 01 / 02 / 04 相关项勾选。

## 验证

- `PromptEngineeringLiveTest`：Few-shot 分类、CoT 计算、企业工单分诊通过。
- `MemoryLiveTest`：t1 记忆 O1001、t2 记忆 O2001，租户隔离通过。
- `TenantMemoryStoreTest`：同一租户复用同一记忆、不同租户隔离。

## 尚未完成（留到下一部分）

- Memory 长期记忆持久化到 Redis / MySQL。
- 部署与工程化中的灰度发布、完整 CI/CD、成本控制。
- Week7~8 企业级项目实战。

## 下一步

- 开始第二部分：为缺失企业级落地案例的笔记补强。
