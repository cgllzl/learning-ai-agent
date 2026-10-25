import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { LoginPage } from './LoginPage'

describe('LoginPage', () => {
  it('展示后端账号目录，并只把 accountId 交给登录回调', async () => {
    const onLogin = vi.fn()
    render(<LoginPage
      accounts={[{
        accountId: 'service-a', displayName: '客服小林', tenantId: 'tenant-a',
        tenantName: '华东零售', roles: ['CUSTOMER_SERVICE'],
      }]}
      loading={false}
      error={null}
      onLogin={onLogin}
    />)

    expect(screen.getByText('华东零售 · 客服专员')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /客服小林/ }))
    expect(onLogin).toHaveBeenCalledWith('service-a')
  })
})
