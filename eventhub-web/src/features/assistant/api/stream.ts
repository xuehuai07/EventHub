import { refreshAccessToken } from '../../../shared/api/http'
import { useAuthStore } from '../../../shared/auth/authStore'
import { dispatchEventBlock, type StreamCallbacks } from './sse'

export async function streamAssistantMessage(
  conversationId: number,
  content: string,
  signal: AbortSignal,
  callbacks: StreamCallbacks,
) {
  const request = async (retried: boolean): Promise<void> => {
    const token = useAuthStore.getState().accessToken
    const origin = import.meta.env.VITE_API_ORIGIN || ''
    const response = await fetch(
      `${origin}/api/assistant/conversations/${conversationId}/messages/stream`,
      {
        method: 'POST',
        credentials: 'include',
        signal,
        headers: {
          Accept: 'text/event-stream',
          'Content-Type': 'application/json',
          'X-Request-Id': crypto.randomUUID(),
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ content }),
      },
    )

    if (response.status === 401 && !retried) {
      await refreshAccessToken()
      return request(true)
    }
    if (!response.ok) {
      const body = (await response.json().catch(() => null)) as {
        code?: string
        message?: string
      } | null
      throw new Error(body?.message || `AI 请求失败（${response.status}）`)
    }
    if (!response.body) {
      throw new Error('浏览器未提供流式响应')
    }

    await consumeEventStream(response.body, callbacks)
  }

  await request(false)
}

async function consumeEventStream(
  stream: ReadableStream<Uint8Array>,
  callbacks: StreamCallbacks,
) {
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    buffer += decoder.decode(value, { stream: !done }).replace(/\r\n/g, '\n')
    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      const terminal = dispatchEventBlock(buffer.slice(0, boundary), callbacks)
      buffer = buffer.slice(boundary + 2)
      if (terminal) {
        await reader.cancel()
        return
      }
      boundary = buffer.indexOf('\n\n')
    }
    if (done) break
  }
  if (buffer.trim()) {
    dispatchEventBlock(buffer, callbacks)
  }
}
