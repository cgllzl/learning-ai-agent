import { describe, expect, it } from 'vitest'
import { visibleNavigation } from './permissions'

describe('visibleNavigation', () => {
  it('客服只看到工作入口，不会看到审批、审计或系统治理', () => {
    const keys = visibleNavigation(['CUSTOMER_SERVICE']).map((item) => item.key)
    expect(keys).toEqual(['overview', 'workbench', 'orders', 'knowledge'])
  })

  it('运营主管可以审批，系统管理员可以看到全部模块', () => {
    expect(visibleNavigation(['OPERATIONS_MANAGER']).map((item) => item.key)).toContain('approvals')
    expect(visibleNavigation(['SYSTEM_ADMIN'])).toHaveLength(8)
  })
})
