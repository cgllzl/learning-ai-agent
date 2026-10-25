import type { DemoAccount } from '../types'
import { roleNames } from '../auth/permissions'

interface LoginPageProps {
  accounts: DemoAccount[]
  loading: boolean
  error: string | null
  onLogin: (accountId: string) => void
}

export function LoginPage({ accounts, loading, error, onLogin }: LoginPageProps) {
  return (
    <main className="login-page">
      <section className="login-intro">
        <p className="kicker">ENTERPRISE AI OPERATIONS</p>
        <h1>让每一次 Agent 操作<br />都有身份、边界与证据</h1>
        <p className="intro-copy">
          选择一个后端登记的演示身份进入工作台。租户与角色由服务端会话恢复，
          页面不能自行修改权限。
        </p>
        <div className="trust-list" aria-label="系统能力">
          <span>多租户隔离</span><span>参数级审批</span><span>全链路审计</span>
        </div>
      </section>

      <section className="login-panel" aria-labelledby="login-title">
        <div className="panel-heading">
          <span className="status-dot" />
          <div>
            <p className="kicker">DEMO ACCESS</p>
            <h2 id="login-title">选择演示身份</h2>
          </div>
        </div>
        {error && <div className="error-banner" role="alert">{error}</div>}
        <div className="account-list">
          {loading && <p className="muted">正在读取后端账号目录…</p>}
          {!loading && accounts.map((account) => (
            <button className="account-card" key={account.accountId} onClick={() => onLogin(account.accountId)}>
              <span className="avatar">{account.displayName.slice(-2)}</span>
              <span className="account-copy">
                <strong>{account.displayName}</strong>
                <small>{account.tenantName} · {account.roles.map((role) => roleNames[role]).join(' / ')}</small>
              </span>
              <span aria-hidden="true">→</span>
            </button>
          ))}
        </div>
        <p className="security-note">会话令牌仅保存在当前标签页，服务重启后自动失效。</p>
      </section>
    </main>
  )
}
