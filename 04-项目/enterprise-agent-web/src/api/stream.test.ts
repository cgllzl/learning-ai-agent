import { afterEach, describe, expect, it, vi } from 'vitest'
import { SseParser, streamWorkbenchQuery } from './stream'

describe('工作台 SSE', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('跨网络分块拼出事件，并携带 Bearer 而不提交租户', async () => {
    const encoder = new TextEncoder()
    const fetchMock = vi.fn().mockResolvedValue(new Response(new ReadableStream({
      start(controller) {
        controller.enqueue(encoder.encode('event:delta\ndata:{"type":"delta","traceId":"t1","sequence":1,"text":"你'))
        controller.enqueue(encoder.encode('好"}\n\nevent:done\ndata:{"type":"done","traceId":"t1","sequence":2,"status":"COMPLETED"}\n\n'))
        controller.close()
      },
    }), { status: 200, headers: { 'Content-Type': 'text/event-stream' } }))
    vi.stubGlobal('fetch', fetchMock)
    const events: string[] = []

    await streamWorkbenchQuery('token', '售后期限', 'KNOWLEDGE', new AbortController().signal,
      (event) => events.push(event.type))

    expect(events).toEqual(['delta', 'done'])
    const [path, init] = fetchMock.mock.calls[0]
    expect(path).toBe('/api/v1/workbench/query/stream')
    expect(init.headers.Authorization).toBe('Bearer token')
    expect(JSON.parse(init.body)).toEqual({ message: '售后期限', mode: 'KNOWLEDGE' })
  })

  it('断线时有部分文字也必须判为失败，且解析器不会提前解析半帧', async () => {
    const parser = new SseParser()
    expect(parser.push('event:delta\ndata:{"type":"delta"')).toEqual([])
    expect(parser.push('}\n\n')).toHaveLength(1)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      'event:delta\ndata:{"type":"delta","traceId":"t1","sequence":1,"text":"半段"}\n\n',
      { status: 200, headers: { 'Content-Type': 'text/event-stream' } },
    )))
    await expect(streamWorkbenchQuery('token', 'hi', 'GENERAL', new AbortController().signal,
      () => undefined)).rejects.toThrow('STREAM_DISCONNECTED')
  })
})
