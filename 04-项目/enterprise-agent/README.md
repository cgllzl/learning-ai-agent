# enterprise-agent

企业级 AI Agent 平台（Enterprise AI Knowledge & Operations Agent）——Day 1 阶段。

- 技术栈：Java 21 / Spring Boot 3.5.16 / Maven / LangChain4j 1.18.1（DeepSeek）
- 当前状态：环境就绪，空项目可启动，`/actuator/health` 返回 UP
- 学习配套：知识库根目录 `F:\ChatGPT\学习之路`（本目录即知识库内 `04-项目\enterprise-agent`）

## 环境要求

- JDK 21（本机：`G:\Environment\Java\temurin-21`，需将 `JAVA_HOME` 指向它）
- Maven 3.8+（已装：`G:\Environment\Maven\apache-maven-3.8.4`）

## 快速开始

```powershell
# 1. 配置 DeepSeek API Key（Day 2 的 /chat 需要）
#    复制 .env.example 为 .env，填入 DEEPSEEK_API_KEY

# 2. 启动（自动读取 .env）
.\scripts\run-dev.ps1

# 3. 验证
#    http://localhost:8080/actuator/health  → {"status":"UP"}
```

## 配置说明

| 配置项 | 位置 | 说明 |
| --- | --- | --- |
| `DEEPSEEK_API_KEY` | `.env`（不入库） | DeepSeek API Key，从 https://platform.deepseek.com 获取 |
| `deepseek.base-url` | `application.yml` | 默认 `https://api.deepseek.com` |
| `deepseek.model` | `application.yml` | 默认 `deepseek-chat` |
| 服务端口 | `application.yml` | 默认 `8080` |

## 目录结构

```text
src/main/java/com/enterprise/agent/   主程序
src/main/resources/application.yml    配置
scripts/run-dev.ps1                   本地启动脚本（读取 .env）
scripts/install-docker.ps1            一键安装 Docker Desktop（需管理员）
```

## 里程碑

- Day 1（2026-08-11）：环境搭建完成，空项目跑通（本阶段）
- Day 2：第一个 `/chat` 接口（LLM API / Streaming / Structured Output）
