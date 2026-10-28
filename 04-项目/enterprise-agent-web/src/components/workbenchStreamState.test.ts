import { describe, expect, it } from 'vitest'
import { applyStreamEvent, initialStreamState } from './workbenchStreamState'
import type { StreamEvent } from '../types'

function event(type: StreamEvent['type'], sequence: number, text: string | null = null): StreamEvent {
  return { type, sequence, text, traceId: 't1', citations: null, status: null, errorCode: null }
}

describe('流式页面状态', () => {
  it('追加增量文本，忽略重复序号，完成后不再接受迟到的片段', () => {
    const running = { ...initialStreamState, status: 'running' as const }
    const first = applyStreamEvent(running, event('delta', 1, '你'))
    expect(applyStreamEvent(first, event('delta', 1, '重复'))).toBe(first)
    const done = applyStreamEvent(first, { ...event('done', 2), status: 'COMPLETED' })
    expect(done.answer).toBe('你')
    expect(done.status).toBe('completed')
    expect(applyStreamEvent(done, event('delta', 3, '迟到'))).toBe(done)
  })
})
