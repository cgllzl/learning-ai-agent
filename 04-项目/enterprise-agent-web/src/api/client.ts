import type {
  DemoAccount,
  DemoPrincipal,
  LoginResponse,
  RouteResult,
  StoredSession,
} from '../types'

const SESSION_KEY = 'enterprise-agent.demo-session'
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string) {
    super(code)
    this.status = status
    this.code = code
  }
}

async function request<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers })
  if (!response.ok) {
    const body = await response.json().catch(() => ({ error: 'REQUEST_FAILED' }))
    throw new ApiError(response.status, body.error ?? 'REQUEST_FAILED')
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  accounts: () => request<DemoAccount[]>('/api/v1/auth/demo-accounts'),
  login: (accountId: string) => request<LoginResponse>('/api/v1/auth/demo-login', {
    method: 'POST',
    body: JSON.stringify({ accountId }),
  }),
  me: (token: string) => request<DemoPrincipal>('/api/v1/auth/me', {}, token),
  logout: (token: string) => request<void>('/api/v1/auth/logout', { method: 'POST' }, token),
  route: (token: string, message: string) => request<RouteResult>(
    '/api/v1/workbench/route',
    { method: 'POST', body: JSON.stringify({ message }) },
    token,
  ),
}

// Token 只保存在当前浏览器标签页；后端仍是身份与角色的唯一事实来源。
export const sessionStore = {
  load(): StoredSession | null {
    const raw = sessionStorage.getItem(SESSION_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw) as StoredSession
    } catch {
      sessionStorage.removeItem(SESSION_KEY)
      return null
    }
  },
  save(session: StoredSession) {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
  },
  clear() {
    sessionStorage.removeItem(SESSION_KEY)
  },
}
