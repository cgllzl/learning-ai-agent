import type { DemoRole, ViewKey } from '../types'

export interface NavigationItem {
  key: ViewKey
  label: string
  eyebrow: string
  roles?: DemoRole[]
}

export const navigation: NavigationItem[] = [
  { key: 'overview', label: '运营总览', eyebrow: 'OV' },
  { key: 'workbench', label: 'AI 工作台', eyebrow: 'AI' },
  { key: 'orders', label: '订单中心', eyebrow: 'OR', roles: ['CUSTOMER_SERVICE', 'OPERATIONS_MANAGER', 'SYSTEM_ADMIN'] },
  { key: 'knowledge', label: '知识库', eyebrow: 'KB', roles: ['CUSTOMER_SERVICE', 'KNOWLEDGE_ADMIN', 'SYSTEM_ADMIN'] },
  { key: 'approvals', label: '审批中心', eyebrow: 'AP', roles: ['OPERATIONS_MANAGER', 'SYSTEM_ADMIN'] },
  { key: 'audit', label: 'Trace 与审计', eyebrow: 'TR', roles: ['AUDITOR', 'SYSTEM_ADMIN'] },
  { key: 'evaluation', label: 'Evaluation', eyebrow: 'EV', roles: ['AUDITOR', 'SYSTEM_ADMIN'] },
  { key: 'governance', label: 'MCP / Skill', eyebrow: 'MC', roles: ['SYSTEM_ADMIN'] },
]

export function visibleNavigation(roles: DemoRole[]): NavigationItem[] {
  return navigation.filter((item) =>
    !item.roles || item.roles.some((role) => roles.includes(role)),
  )
}

export const roleNames: Record<DemoRole, string> = {
  CUSTOMER_SERVICE: '客服专员',
  OPERATIONS_MANAGER: '运营主管',
  KNOWLEDGE_ADMIN: '知识管理员',
  AUDITOR: '审计员',
  SYSTEM_ADMIN: '系统管理员',
}
