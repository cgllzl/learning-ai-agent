import { useMemo, useState } from 'react'
import { api } from '../api/client'
import { roleNames, visibleNavigation } from '../auth/permissions'
import type { RouteResult, StoredSession, ViewKey } from '../types'

interface WorkspaceShellProps {
  session: StoredSession
  onLogout: () => void
}

const pageCopy: Partial<Record<ViewKey, { title: string; description: string }>> = {
  orders: { title: '订单中心', description: 'Day 4 接入分页、物流时间线和受控售后入口。' },
  knowledge: { title: '知识库', description: 'Day 5 接入文档权限、索引状态和引用查看。' },
  approvals: { title: '审批中心', description: 'Day 6 接入参数差异、过期与消费状态。' },
  audit: { title: 'Trace 与审计', description: 'Day 7 接入脱敏的模型与 Tool 调用链。' },
  evaluation: { title: 'Evaluation', description: 'Day 7 展示成功率、P95、成本和失败原因。' },
  governance: { title: 'MCP / Skill 治理', description: 'Day 7 展示审核状态、Tool 指纹和 Skill 版本。' },
}

export function WorkspaceShell({ session, onLogout }: WorkspaceShellProps) {
  const menu = useMemo(() => visibleNavigation(session.principal.roles), [session.principal.roles])
  const [activeView, setActiveView] = useState<ViewKey>('overview')
  const [message, setMessage] = useState('查询订单 O1001 的金额和状态')
  const [route, setRoute] = useState<RouteResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [routing, setRouting] = useState(false)

  async function previewRoute() {
    setRouting(true)
    setError(null)
    try {
      setRoute(await api.route(session.accessToken, message))
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'REQUEST_FAILED')
    } finally {
      setRouting(false)
    }
  }

  return (
    <div className="workspace-shell">
      <aside className="sidebar">
        <div className="brand-mark">EA</div>
        <div className="brand-copy"><strong>Enterprise AI</strong><span>Operations Copilot</span></div>
        <nav aria-label="工作台导航">
          {menu.map((item) => (
            <button
              key={item.key}
              className={activeView === item.key ? 'nav-item active' : 'nav-item'}
              onClick={() => setActiveView(item.key)}
            >
              <span>{item.eyebrow}</span>{item.label}
            </button>
          ))}
        </nav>
        <div className="sidebar-foot"><small>Trusted tenant</small><strong>{session.principal.tenantId}</strong></div>
      </aside>

      <main className="workspace-main">
        <header className="topbar">
          <div>
            <p className="kicker">{session.principal.tenantName}</p>
            <h1>{menu.find((item) => item.key === activeView)?.label ?? '工作台'}</h1>
          </div>
          <div className="identity-chip">
            <div><strong>{session.principal.displayName}</strong><span>{session.principal.roles.map((role) => roleNames[role]).join(' / ')}</span></div>
            <button onClick={onLogout}>退出</button>
          </div>
        </header>

        {error && <div className="error-banner" role="alert">请求失败：{error}</div>}
        {activeView === 'overview' && <Overview />}
        {activeView === 'workbench' && (
          <section className="content-grid">
            <div className="surface route-form">
              <p className="kicker">ROUTE PREVIEW</p>
              <h2>先判断问题该去哪里</h2>
              <p className="muted">路由只提供页面建议，不会查询或修改业务数据。</p>
              <label htmlFor="route-message">用户问题</label>
              <textarea id="route-message" value={message} onChange={(event) => setMessage(event.target.value)} />
              <button className="primary-button" onClick={previewRoute} disabled={routing || !message.trim()}>
                {routing ? '正在判断…' : '预判路由'}
              </button>
            </div>
            <div className="surface result-panel" aria-live="polite">
              <p className="kicker">TRUSTED RESULT</p>
              <h2>后端返回</h2>
              {route ? (
                <dl>
                  <div><dt>类别</dt><dd>{route.category}</dd></div>
                  <div><dt>建议页面</dt><dd>{route.suggestedView}</dd></div>
                  <div><dt>需要人工</dt><dd>{route.requiresHumanReview ? '是' : '否'}</dd></div>
                  <div><dt>执行状态</dt><dd className="safe-state">{route.executionStatus}</dd></div>
                </dl>
              ) : <p className="muted">提交问题后，这里显示 Java 收敛后的页面建议。</p>}
            </div>
          </section>
        )}
        {pageCopy[activeView] && <Placeholder {...pageCopy[activeView]!} />}
      </main>
    </div>
  )
}

function Overview() {
  return (
    <>
      <section className="metric-grid">
        <Metric label="当前阶段" value="P1 · Day 2" note="身份与前端壳" />
        <Metric label="受信身份" value="已接入" note="Bearer → 服务端会话" />
        <Metric label="路由执行" value="0 次" note="当前仅预判页面" />
        <Metric label="待审批" value="Day 6" note="后续业务接入" />
      </section>
      <section className="surface roadmap-panel">
        <p className="kicker">DELIVERY MAP</p>
        <h2>企业主链路建设进度</h2>
        <div className="roadmap-row">
          <span className="done">D1 路由契约</span><span className="current">D2 身份与界面</span>
          <span>D3 流式工作台</span><span>D4 订单</span><span>D5 知识</span><span>D6 审批</span><span>D7 治理</span>
        </div>
      </section>
    </>
  )
}

function Metric({ label, value, note }: { label: string; value: string; note: string }) {
  return <article className="surface metric"><span>{label}</span><strong>{value}</strong><small>{note}</small></article>
}

function Placeholder({ title, description }: { title: string; description: string }) {
  return <section className="surface empty-state"><span>BUILDING NEXT</span><h2>{title}</h2><p>{description}</p></section>
}
