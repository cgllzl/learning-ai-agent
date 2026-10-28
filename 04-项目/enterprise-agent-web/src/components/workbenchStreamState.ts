import type { StreamEvent, WorkbenchCitation } from '../types'

export interface StreamViewState {
  status: 'idle' | 'running' | 'completed' | 'failed' | 'stopped'
  answer: string
  steps: string[]
  citations: WorkbenchCitation[]
  traceId: string | null
  lastSequence: number
  errorCode: string | null
}

export const initialStreamState: StreamViewState = {
  status: 'idle', answer: '', steps: [], citations: [],
  traceId: null, lastSequence: 0, errorCode: null,
}

export function applyStreamEvent(state: StreamViewState, event: StreamEvent): StreamViewState {
  if (state.status !== 'running' || event.sequence <= state.lastSequence) return state
  if (state.traceId && state.traceId !== event.traceId) return state
  const next = { ...state, traceId: event.traceId, lastSequence: event.sequence }
  switch (event.type) {
    case 'step': return { ...next, steps: [...next.steps, event.text ?? ''] }
    case 'citations': return { ...next, citations: event.citations ?? [] }
    case 'delta': return { ...next, answer: next.answer + (event.text ?? '') }
    case 'done': return {
      ...next,
      status: event.status === 'COMPLETED' ? 'completed' : 'failed',
      errorCode: event.errorCode ?? null,
    }
    default: return next
  }
}
