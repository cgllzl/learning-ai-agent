# Enterprise Agent Web

Phase 2 的企业工作台前端。Day 3 已实现演示身份登录、按角色导航、SSE 流式回答、演示引用与步骤、停止/重试、断线失败状态，并保留 Day 2 路由预览。

## 本地运行

先启动后端 `enterprise-agent`（8080），再在本目录执行：

```powershell
npm install
npm run dev -- --configLoader native
```

Vite 开发服务使用 5173 端口，并将 `/api` 代理到 `http://localhost:8080`。本机中文目录使用 `--configLoader native` 避免配置加载路径问题。如直接访问其他后端地址，设置 `VITE_API_BASE_URL`。

## 验证

```powershell
npm test -- --configLoader native
npm run build -- --configLoader native
```

Vite 只负责转换 TypeScript；`npm run build` 先执行 `tsc -b` 做完整类型检查，再生成生产资源。[React 官方从零构建说明](https://react.dev/learn/build-a-react-app-from-scratch)、[Vite 官方指南](https://vite.dev/guide/)
