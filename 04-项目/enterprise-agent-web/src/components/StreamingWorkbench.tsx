import { useEffect, useRef, useState } from 'react'
import { streamWorkbenchQuery } from '../api/stream'
import type { StoredSession, WorkbenchMode } from '../types'
import { applyStreamEvent, initialStreamState, type StreamViewState } from './workbenchStreamState'

export function StreamingWorkbench({ session }: { session: StoredSession }) {
  const [message, setMessage] = useState('售后申请期限是什么？')
  const [mode, setMode] = useState<WorkbenchMode>('KNOWLEDGE')
  const [view, setView] = useState<StreamViewState>(initialStreamState)
  const controller = useRef<AbortController | null>(null)
  const busy = useRef(false)
  const lastRequest = useRef<{ message: string; mode: WorkbenchMode } | null>(null)

  useEffect(() => () => controller.current?.abort(), [])

  async function submit(input = { message, mode }) {
    if (busy.current || !input.message.trim()) return
    busy.current = true
    lastRequest.current = input
    const nextController = new AbortController()
    controller.current = nextController
    setView({ ...initialStreamState, status: 'running' })
    try {
      await streamWorkbenchQuery(session.accessToken, input.message, input.mode,
        nextController.signal, (event) => setView((current) => applyStreamEvent(current, event)))
    } catch (error) {
      if (!nextController.signal.aborted) {
        setView((current) => ({ ...current, status: 'failed',
          errorCode: error instanceof Error ? error.message : 'REQUEST_FAILED' }))
      }
    } finally {
      if (controller.current === nextController) controller.current = null
      busy.current = false
    }
  }

  function stop() {
    controller.current?.abort()
    setView((current) => current.status === 'running'
      ? { ...current, status: 'stopped' } : current)
  }

  return (
    <section className="stream-workbench">
      <div className="surface route-form">
        <p className="kicker">LIVE ANSWER</p>
        <h2>流式 AI 工作台</h2>
        <p className="muted">知识模式只使用当前租户的演示资料；通用模式不查询或修改业务记录。</p>
        <label htmlFor="query-mode">回答模式</label>
        <select id="query-mode" value={mode} onChange={(event) => setMode(event.target.value as WorkbenchMode)}>
          <option value="KNOWLEDGE">知识问答（演示资料）</option>
          <option value="GENERAL">通用解释</option>
        </select>
        <label htmlFor="query-message">用户问题</label>
        <textarea id="query-message" value={message} onChange={(event) => setMessage(event.target.value)} maxLength={2000} />
        <div className="stream-actions">
          <button className="primary-button" onClick={() => void submit()} disabled={view.status === 'running' || !message.trim()}>发送问题</button>
          <button onClick={stop} disabled={view.status !== 'running'}>停止显示</button>
          <button onClick={() => lastRequest.current && void submit(lastRequest.current)}
            disabled={view.status === 'running' || !lastRequest.current}>重试上次问题</button>
        </div>
        <p className="muted">停止会中断浏览器接收；已发出的模型请求可能仍会计费。重试会重新调用模型。</p>
      </div>
      <div className="surface result-panel" aria-live="polite">
        <p className="kicker">EXECUTION TRACE</p>
        <h2>回答与过程</h2>
        <p className="stream-status">状态：{view.status}{view.errorCode ? ` · ${view.errorCode}` : ''}</p>
        {view.traceId && <p className="trace-id">traceId：{view.traceId}</p>}
        <ol className="stream-steps">{view.steps.map((step, index) => <li key={index}>{step}</li>)}</ol>
        <div className="stream-answer">{view.answer || '发送问题后，回答会逐段显示在这里。'}</div>
        <h3>参考来源</h3>
        {view.citations.length ? view.citations.map((source) => (
          <article className="citation" key={source.sourceId}>
            <strong>[1] {source.title}</strong><p>{source.excerpt}</p><small>{source.sourceId}</small>
          </article>
        )) : <p className="muted">本次没有可展示的来源。</p>}
      </div>
    </section>
  )
}
