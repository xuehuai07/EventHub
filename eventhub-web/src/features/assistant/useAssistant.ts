import { useCallback, useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { ResourceCard } from '../../shared/api/generated/types.gen'
import { useAuthStore } from '../../shared/auth/authStore'
import {
  createConversation,
  deleteConversation,
  getAssistantConversations,
  getAssistantMessages,
  renameConversation,
  streamAssistantMessage,
  type ConversationView,
} from './api'
import type { UiMessage } from './types'

export function useAssistant() {
  const navigate = useNavigate()
  const location = useLocation()
  const authenticated = useAuthStore(
    (state) => state.status === 'authenticated' && Boolean(state.user?.id),
  )
  const [open, setOpen] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [conversations, setConversations] = useState<ConversationView[]>([])
  const [activeId, setActiveId] = useState<number | null>(null)
  const [messages, setMessages] = useState<UiMessage[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lastPrompt, setLastPrompt] = useState<string | null>(null)
  const abortRef = useRef<AbortController | null>(null)
  const scrollRef = useRef<HTMLDivElement | null>(null)

  const refreshConversations = useCallback(async () => {
    const page = await getAssistantConversations()
    setConversations(page?.items || [])
  }, [])

  useEffect(() => {
    scrollRef.current?.scrollTo({
      top: scrollRef.current.scrollHeight,
      behavior: generating ? 'smooth' : 'auto',
    })
  }, [messages, generating])

  useEffect(
    () => () => {
      abortRef.current?.abort()
    },
    [],
  )

  const openPanel = () => {
    setOpen(true)
    if (authenticated) {
      void refreshConversations().catch(() => setError('暂时无法读取对话历史'))
    }
  }

  const closePanel = () => setOpen(false)

  const selectConversation = async (conversationId: number) => {
    if (!authenticated || generating) return
    setLoading(true)
    setError(null)
    try {
      const history = await getAssistantMessages(conversationId)
      setActiveId(conversationId)
      setMessages(history || [])
      setHistoryOpen(false)
    } catch {
      setError('读取对话失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  const startNewConversation = () => {
    if (generating) return
    setActiveId(null)
    setMessages([])
    setInput('')
    setError(null)
    setHistoryOpen(false)
  }

  const send = async (prompt = input.trim()) => {
    if (!prompt || generating) return
    if (!authenticated) {
      goToLogin()
      return
    }
    setError(null)
    setLastPrompt(prompt)
    setInput('')
    setGenerating(true)

    let conversationId = activeId
    try {
      if (!conversationId) {
        const created = await createConversation(titleFrom(prompt))
        if (!created?.id) throw new Error('创建对话失败')
        conversationId = created.id
        setActiveId(created.id)
        setConversations((current) => [created, ...current])
      }

      const userLocalId = crypto.randomUUID()
      const assistantLocalId = crypto.randomUUID()
      setMessages((current) => [
        ...current,
        {
          localId: userLocalId,
          role: 'USER',
          content: prompt,
          resources: [],
        },
        {
          localId: assistantLocalId,
          role: 'ASSISTANT',
          content: '',
          resources: [],
          pending: true,
        },
      ])

      const controller = new AbortController()
      abortRef.current = controller
      await streamAssistantMessage(conversationId, prompt, controller.signal, {
        onAck: ({ userMessageId }) =>
          updateMessage(userLocalId, { id: userMessageId }),
        onDelta: (content) =>
          setMessages((current) =>
            current.map((message) =>
              message.localId === assistantLocalId
                ? { ...message, content: `${message.content || ''}${content}` }
                : message,
            ),
          ),
        onResources: (resources) =>
          updateMessage(assistantLocalId, { resources }),
        onDone: ({ assistantMessageId, model }) => {
          updateMessage(assistantLocalId, {
            id: assistantMessageId,
            model,
            pending: false,
          })
          setGenerating(false)
        },
        onError: ({ message }) => {
          setError(message)
          setGenerating(false)
          setMessages((current) =>
            current.map((item) =>
              item.localId === assistantLocalId
                ? {
                    ...item,
                    content: item.content || '本次回答未完成。',
                    pending: false,
                    failed: true,
                  }
                : item,
            ),
          )
        },
      })
      await refreshConversations()
    } catch (caught) {
      if (!(caught instanceof DOMException && caught.name === 'AbortError')) {
        setError(
          caught instanceof Error ? caught.message : 'AI 请求失败，请重试',
        )
      }
    } finally {
      abortRef.current = null
      setGenerating(false)
    }
  }

  const updateMessage = (localId: string, patch: Partial<UiMessage>) => {
    setMessages((current) =>
      current.map((message) =>
        message.localId === localId ? { ...message, ...patch } : message,
      ),
    )
  }

  const stop = () => {
    abortRef.current?.abort()
    setGenerating(false)
    setMessages((current) =>
      current.map((message) =>
        message.pending
          ? {
              ...message,
              content: message.content || '已停止生成。',
              pending: false,
              failed: true,
            }
          : message,
      ),
    )
  }

  const renameActive = async () => {
    if (!authenticated || !activeId) return
    const current = conversations.find((item) => item.id === activeId)
    const title = window.prompt('修改对话名称', current?.title || '')
    if (!title?.trim()) return
    const updated = await renameConversation(activeId, title.trim())
    setConversations((items) =>
      items.map((item) => (item.id === activeId ? updated || item : item)),
    )
  }

  const removeActive = async () => {
    if (
      !authenticated ||
      !activeId ||
      !window.confirm('删除这段对话及全部消息？')
    )
      return
    await deleteConversation(activeId)
    setConversations((items) => items.filter((item) => item.id !== activeId))
    startNewConversation()
  }

  const openResource = (resource: ResourceCard) => {
    if (!resource.href?.startsWith('/')) return
    navigate(resource.href)
    closePanel()
  }

  const goToLogin = () => {
    closePanel()
    navigate('/login', { state: { from: location.pathname } })
  }

  return {
    authenticated,
    open,
    historyOpen,
    conversations,
    activeId,
    messages,
    input,
    loading,
    generating,
    error,
    lastPrompt,
    scrollRef,
    openPanel,
    closePanel,
    setHistoryOpen,
    setInput,
    selectConversation,
    startNewConversation,
    send,
    stop,
    renameActive,
    removeActive,
    openResource,
    goToLogin,
  }
}

export type AssistantController = ReturnType<typeof useAssistant>

function titleFrom(prompt: string) {
  return prompt.replace(/\s+/g, ' ').slice(0, 30)
}
