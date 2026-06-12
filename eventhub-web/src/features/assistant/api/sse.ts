import type { ResourceCard } from '../../../shared/api/generated/types.gen'

export interface StreamCallbacks {
  onAck?: (payload: { conversationId: number; userMessageId: number }) => void
  onDelta: (content: string) => void
  onResources: (resources: ResourceCard[]) => void
  onDone: (payload: {
    assistantMessageId: number
    model: string
    promptTokens: number
    completionTokens: number
  }) => void
  onError: (payload: {
    code: string
    message: string
    retryable: boolean
  }) => void
}

export function dispatchEventBlock(block: string, callbacks: StreamCallbacks) {
  let event = 'message'
  const data: string[] = []
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    if (line.startsWith('data:')) data.push(line.slice(5).trimStart())
  }
  if (!data.length) return false

  const payload = JSON.parse(data.join('\n')) as Record<string, unknown>
  switch (event) {
    case 'ack':
      callbacks.onAck?.(
        payload as { conversationId: number; userMessageId: number },
      )
      break
    case 'delta':
      callbacks.onDelta(String(payload.content || ''))
      break
    case 'resources':
      callbacks.onResources((payload.items || []) as ResourceCard[])
      break
    case 'done':
      callbacks.onDone(
        payload as {
          assistantMessageId: number
          model: string
          promptTokens: number
          completionTokens: number
        },
      )
      return true
    case 'error':
      callbacks.onError(
        payload as {
          code: string
          message: string
          retryable: boolean
        },
      )
      return true
  }
  return false
}
