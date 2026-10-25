import { useEffect, useState } from 'react'
import { api, sessionStore } from './api/client'
import { LoginPage } from './components/LoginPage'
import { WorkspaceShell } from './components/WorkspaceShell'
import type { DemoAccount, StoredSession } from './types'

export default function App() {
  const [session, setSession] = useState<StoredSession | null>(() => sessionStore.load())
  const [accounts, setAccounts] = useState<DemoAccount[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    // 启动时先取后端账号目录；如标签页已有 Token，再用 /me 重新确认身份。
    async function bootstrap() {
      try {
        setAccounts(await api.accounts())
        if (session) {
          const principal = await api.me(session.accessToken)
          const refreshed = { ...session, principal }
          setSession(refreshed)
          sessionStore.save(refreshed)
        }
      } catch (bootstrapError) {
        if (session) {
          sessionStore.clear()
          setSession(null)
        }
        setError(bootstrapError instanceof Error ? bootstrapError.message : 'BACKEND_UNAVAILABLE')
      } finally {
        setLoading(false)
      }
    }
    void bootstrap()
    // 首次加载验证一次已有会话；后续身份变化由登录和退出显式处理。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function login(accountId: string) {
    setLoading(true)
    setError(null)
    try {
      const nextSession = await api.login(accountId)
      sessionStore.save(nextSession)
      setSession(nextSession)
    } catch (loginError) {
      setError(loginError instanceof Error ? loginError.message : 'LOGIN_FAILED')
    } finally {
      setLoading(false)
    }
  }

  async function logout() {
    if (session) await api.logout(session.accessToken).catch(() => undefined)
    sessionStore.clear()
    setSession(null)
  }

  if (session) return <WorkspaceShell session={session} onLogout={logout} />
  return <LoginPage accounts={accounts} loading={loading} error={error} onLogin={login} />
}
