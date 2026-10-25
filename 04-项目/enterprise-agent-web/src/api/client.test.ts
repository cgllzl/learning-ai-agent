import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from './client'

describe('api client', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('受保护请求携带 Bearer Token，且租户不从请求体提交', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      category: 'ORDER', suggestedView: 'ORDERS',
      requiresHumanReview: false, executionStatus: 'NOT_EXECUTED',
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    vi.stubGlobal('fetch', fetchMock)

    await api.route('server-token', '查询订单 O1001')

    const [path, init] = fetchMock.mock.calls[0]
    expect(path).toBe('/api/v1/workbench/route')
    expect((init.headers as Headers).get('Authorization')).toBe('Bearer server-token')
    expect(JSON.parse(init.body as string)).toEqual({ message: '查询订单 O1001' })
  })
})
