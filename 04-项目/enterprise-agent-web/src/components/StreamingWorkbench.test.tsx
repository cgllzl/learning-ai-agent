import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { StreamingWorkbench } from './StreamingWorkbench'
import { streamWorkbenchQuery } from '../api/stream'
import type { StoredSession } from '../types'

vi.mock('../api/stream', () => ({ streamWorkbenchQuery: vi.fn() }))

const session: StoredSession = {
  tokenType: 'Bearer', accessToken: 'test-token', expiresAt: 'tomorrow',
  principal: { userId: 'u1', displayName: '客服', tenantId: 'tenant-a',
    tenantName: '华东零售', roles: ['CUSTOMER_SERVICE'] },
}

describe('流式工作台操作', () => {
  it('进行中的请求不能重复提交，停止后可重试上次问题', async () => {
    const calls = vi.mocked(streamWorkbenchQuery).mockImplementation((_token, _message, _mode, signal) =>
      new Promise<void>((_resolve, reject) => signal.addEventListener('abort',
        () => reject(new DOMException('aborted', 'AbortError')))))
    const user = userEvent.setup()
    render(<StreamingWorkbench session={session} />)

    await user.click(screen.getByRole('button', { name: '发送问题' }))
    expect(calls).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: '发送问题' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: '停止显示' }))
    expect(await screen.findByText('状态：stopped')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '重试上次问题' }))
    expect(calls).toHaveBeenCalledTimes(2)
    expect(calls.mock.calls[1][1]).toBe('售后申请期限是什么？')
  })
})
