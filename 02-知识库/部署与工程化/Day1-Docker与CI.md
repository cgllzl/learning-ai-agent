# Day 1：Docker 化与部署工程（补全部署与工程化模块）

> 补全日期：2026-09-05 ｜ 交付：`04-项目/enterprise-agent/Dockerfile`、`docker-compose.yml`

## 一、先讲人话：为什么要把项目 Docker 化

本地能跑，不代表别人能跑。不同机器 JDK 版本、Maven 版本、环境变量都可能不一样。Docker 把这些都装进一个容器，做到「构建一次，到处运行」。

## 二、多阶段构建：先打包，再运行时

`Dockerfile` 分两段：

1. `build` 阶段：用 Maven + JDK 21 把项目打成 jar。
2. `runtime` 阶段：只放 JRE 和 jar，镜像更小，也更安全。

```dockerfile
FROM maven:3.8.4-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
COPY --from=build /app/target/enterprise-agent-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 三、docker-compose 负责运行参数

`docker-compose.yml` 把端口、密钥注入集中管理：

```yaml
services:
  enterprise-agent:
    build: .
    ports:
      - "8080:8080"
    env_file:
      - .env
```

密钥仍从 `.env` 读取，不写进镜像或仓库，延续 Secret 管理原则。

## 四、部署后的企业例子

容器启动后，`/actuator/health` 应返回 UP；再用真实 DeepSeek 跑一次冒烟：

```powershell
docker compose up --build -d
curl.exe http://localhost:8080/actuator/health
.\scripts\test-live.ps1 -Test PromptEngineeringLiveTest
```

这个企业例子回答：**新机器上能不能一键拉起服务，并且拉起后大模型链路还正常。**

## 五、尚未实现的生产能力（Week 7~8 再补）

- dev / staging / prod 环境划分
- 灰度发布与回滚
- 完整 CI/CD（当前已有评估 CI，但缺构建→部署）
- 成本控制（限流、缓存）

## 六、完成标准

- [x] 有 Dockerfile 与 docker-compose
- [x] 密钥通过环境变量注入
- [x] 写明部署后的企业冒烟场景
