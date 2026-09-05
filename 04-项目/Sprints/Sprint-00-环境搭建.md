# Sprint 0：环境搭建

> 状态：2026-08-11 完成（Day 1）｜ 归档：`05-记录/归档/2026-08-11-Day1-环境搭建.md`

## 目标
从零搭起可运行的项目骨架。

## 任务
- [x] 安装 / 确认 JDK 17+（Temurin 21.0.12，`G:\Environment\Java\temurin-21`）
- [x] Maven 切换 Java 21（Maven 3.8.4）
- [x] 创建 Spring Boot 3.5.16 项目（手写 pom，start.spring.io 已不支持 3.5.x）
- [x] 加入 LangChain4j 依赖（langchain4j-open-ai 1.18.1）
- [x] 配置 LLM API Key（DeepSeek，`.env` 存 DEEPSEEK_API_KEY，不入库）
- [x] Docker 化（已生成 `scripts/install-docker.ps1`，并补齐 Dockerfile 与 docker-compose.yml）
- [x] `/actuator/health` 可用，项目 README 写明启动方式

## 技术
JDK 21 / Spring Boot 3.5.16 / Maven / LangChain4j 1.18.1 / Docker（待装）

## 知识库映射
- `02-知识库/部署与工程化/`：`本机环境.md`（JDK/Docker）
- `02-知识库/LLM应用开发/`：`DeepSeek配置.md`
- 资料源：LangChain4j Getting Started、Spring Boot 文档、DeepSeek API 文档

## 完成标准
- [x] 项目可本地启动（`mvn spring-boot:run`）
- [x] `/actuator/health` 返回 200 + `{"status":"UP"}`
- [ ] `docker compose up` 一键启动（文件已就绪，待实际启动验证）
