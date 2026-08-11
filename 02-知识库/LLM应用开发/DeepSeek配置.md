# DeepSeek 配置（Day 1）

> 归档：`05-记录/归档/2026-08-11-Day1-环境搭建.md` ｜ 项目：`04-项目/enterprise-agent`

## 基本信息
- 官网：https://platform.deepseek.com
- API 文档：https://api-docs.deepseek.com
- 接口兼容：OpenAI 兼容，可用 LangChain4j 的 open-ai 模块对接
- 模型：`deepseek-chat`（对话）；另有 `deepseek-reasoner`（推理，后续可试）

## 项目配置
- base-url：`https://api.deepseek.com`
- model：`deepseek-chat`
- api-key：从环境变量 `DEEPSEEK_API_KEY` 读取（存于 `.env`，不入库）

```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY:}
  base-url: https://api.deepseek.com
  model: deepseek-chat
```

## 使用注意
- Key 只能写入 `.env`，禁止提交到 git（`.gitignore` 已排除）
- 未配置 Key 时应用也能启动（Day 1 不调用 API）；Day 2 的 /chat 需要真实 Key
