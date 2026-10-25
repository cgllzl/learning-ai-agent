export type DemoRole =
  | 'CUSTOMER_SERVICE'
  | 'OPERATIONS_MANAGER'
  | 'KNOWLEDGE_ADMIN'
  | 'AUDITOR'
  | 'SYSTEM_ADMIN'

export type ViewKey =
  | 'overview'
  | 'workbench'
  | 'orders'
  | 'knowledge'
  | 'approvals'
  | 'audit'
  | 'evaluation'
  | 'governance'

export interface DemoPrincipal {
  userId: string
  displayName: string
  tenantId: string
  tenantName: string
  roles: DemoRole[]
}

export interface DemoAccount extends Omit<DemoPrincipal, 'userId'> {
  accountId: string
}

export interface LoginResponse {
  tokenType: 'Bearer'
  accessToken: string
  expiresAt: string
  principal: DemoPrincipal
}

export type StoredSession = LoginResponse

export interface RouteResult {
  category: 'ORDER' | 'KNOWLEDGE' | 'HUMAN'
  suggestedView: 'ORDERS' | 'KNOWLEDGE' | 'HUMAN_QUEUE'
  requiresHumanReview: boolean
  executionStatus: 'NOT_EXECUTED'
}
