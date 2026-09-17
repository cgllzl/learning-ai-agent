# Sprint 5：MCP

## 目标
把外部工具通过 MCP 标准接入。

## 任务
- [x] 在 Java 项目里接入 MCP Client（LangChain4j MCP）
- [x] 把一个现有 Tool 改造成 MCP Tool
- [ ] 接入一个第三方 MCP Server 验证
- [x] 记录：什么时候该用 MCP、什么时候不该用
- [x] 建立 2025 旧线 → 2026-07-28 → 当前 Java 支持情况的兼容性矩阵
- [x] 用真实 stdio 回归固定当前 `2025-11-25` 能力边界
- [x] 工具目录新增、删除、描述或参数 Schema 变化时输出差异并重新审核
- [x] 通过 `McpToolProvider.filter` 只向模型暴露审核通过且权限满足的 Tool
- [x] 为远程凭证原型增加 audience/targetHost 配置、最小 Scope、关闭内置日志和禁重定向边界
- [x] 使用本地等价 Server 完成真实 DeepSeek + MCP 安全策略联调

## 技术
MCP Client / JSON-RPC / Tools / Resources / Protocol Compatibility / Tool Manifest Fingerprint / OAuth Audience / Scope

## 知识库映射
- `02-知识库/MCP/`

## 完成标准
- [x] Agent 能调用 MCP 工具完成一次真实操作
- [x] 能明确说明当前 Client/Server 只验证到 `2025-11-25`，不会误报已支持 `2026-07-28`
- [x] 未审核或发生变化的第三方 Tool 不会进入模型可调用列表
- [ ] 使用真实远程第三方 MCP 与 OAuth 完成联调（与企业项目并行）
