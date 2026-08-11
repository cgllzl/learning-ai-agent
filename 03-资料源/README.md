# 资料源（03-资料源）

> 规则：以后所有学习资料都按下面的优先级登记，不随手扔一堆博客。

## 优先级规则

| 级别 | 类型 | 用途 | 例子 |
| --- | --- | --- | --- |
| Level 1 | 官方文档 | 事实来源 | LangChain4j、Spring AI、MCP、OpenAI Platform Docs |
| Level 2 | 官方 GitHub / Reference Implementation | 写代码参考 | langchain4j / langchain4j-examples、modelcontextprotocol |
| Level 3 | 原始论文 | 理解原理 | Agent 综述、RAG、Evaluation 相关论文 |
| Level 4 | 高质量技术文章 | 补充工程实践 | 技术博客、官方博客 |
| Level 5 | Reddit / 社区 | 只用来发现实际踩坑 | r/LangChain、r/LocalLLaMA 等，不作唯一事实来源 |

## 核心官方资料速查

### Java / Spring
- Spring Boot 官方文档：https://docs.spring.io/spring-boot
- Spring AI 官方文档：https://docs.spring.io/spring-ai/reference/

### LangChain4j
- 官方文档：https://docs.langchain4j.dev
- Getting Started：https://docs.langchain4j.dev/tutorials/getting-started
- GitHub：https://github.com/langchain4j/langchain4j
- Examples：https://github.com/langchain4j/langchain4j-examples

### MCP
- 官方文档：https://modelcontextprotocol.io
- 官方规范：https://modelcontextprotocol.io/specification/2025-06-18
- GitHub 组织：https://github.com/modelcontextprotocol

### LLM API
- DeepSeek 平台（注册/充值/Key）：https://platform.deepseek.com
- DeepSeek API 文档：https://api-docs.deepseek.com
- OpenAI Platform Docs：https://platform.openai.com/docs
- OpenAI Prompt Engineering：https://platform.openai.com/docs/guides/prompt-engineering
- Anthropic Docs：https://docs.anthropic.com

### 安全
- OWASP Top 10 for LLM Applications：https://owasp.org/www-project-top-10-for-large-language-model-applications/

### 可观测性
- OpenTelemetry：https://opentelemetry.io/docs

## 论文清单（Level 3）

| 论文 | 链接 | 状态 |
| --- | --- | --- |
| Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks | https://arxiv.org/abs/2005.11401 | 未读 |
| The Rise and Potential of LLM Based Agents: A Survey | https://arxiv.org/abs/2309.07864 | 未读 |
| AI Agents That Matter | https://arxiv.org/abs/2407.01502 | 未读 |
| AgentBench | https://arxiv.org/abs/2308.03688 | 未读 |

## 本机镜像/加速备忘（2026-08-11）
- Adoptium JDK 清华镜像：https://mirrors.tuna.tsinghua.edu.cn/Adoptium/
- Maven Central 直连速度可接受；如变慢可配置阿里云镜像

## 登记方式
- 读到新资料 → 在对应 `01-每周学习/Week-XX/资料.md` 或本文件登记：名称 + 链接 + Level 级别 + 一句话说明。
