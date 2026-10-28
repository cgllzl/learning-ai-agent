import type { StreamEvent, WorkbenchMode } from '../types'
import { ApiError } from './client'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/** SSE 可能在任意字节边界断开，先攒到空行再解析完整事件。 */
export class SseParser {
  private buffer = ''

  push(text: string): StreamEvent[] {
    this.buffer += text
    const events: StreamEvent[] = []
    let boundary = this.buffer.search(/\r?\n\r?\n/)
    while (boundary >= 0) {
      const frame = this.buffer.slice(0, boundary)
      const separator = this.buffer.match(/\r?\n\r?\n/)![0]
      this.buffer = this.buffer.slice(boundary + separator.length)
      const data = frame.split(/\r?\n/)
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).trimStart())
        .join('\n')
      if (data) events.push(JSON.parse(data) as StreamEvent)
      boundary = this.buffer.search(/\r?\n\r?\n/)
    }
    return events
  }
}

export async function streamWorkbenchQuery(
  token: string,
  message: string,
  mode: WorkbenchMode,
  signal: AbortSignal,
  onEvent: (event: StreamEvent) => void,
): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/v1/workbench/query/stream`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream', 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, mode }),
    signal,
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({ error: 'REQUEST_FAILED' }))
    throw new ApiError(response.status, body.error ?? 'REQUEST_FAILED')
  }
  if (!response.body) throw new Error('STREAM_UNAVAILABLE')

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  const parser = new SseParser()
  let finished = false
  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      for (const event of parser.push(decoder.decode(value, { stream: true }))) {
        onEvent(event)
        if (event.type === 'done') finished = true
      }
    }
    for (const event of parser.push(decoder.decode())) {
      onEvent(event)
      if (event.type === 'done') finished = true
    }
    // 断线时即使已有部分文字，也不能把它误标为完整回答。
    if (!finished) throw new Error('STREAM_DISCONNECTED')
  } finally {
    reader.releaseLock()
  }
}
