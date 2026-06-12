import type { ConversationView } from '../api'

interface AssistantHistoryProps {
  conversations: ConversationView[]
  activeId: number | null
  onNew: () => void
  onSelect: (conversationId: number) => void
}

export function AssistantHistory({
  conversations,
  activeId,
  onNew,
  onSelect,
}: AssistantHistoryProps) {
  return (
    <aside className="ai-history">
      <div className="ai-history-heading">
        <strong>最近对话</strong>
        <button type="button" onClick={onNew}>
          + 新对话
        </button>
      </div>
      <div className="ai-history-list">
        {conversations.map((conversation) => (
          <button
            className={conversation.id === activeId ? 'is-active' : ''}
            key={conversation.id}
            type="button"
            onClick={() => conversation.id && onSelect(conversation.id)}
          >
            <span>{conversation.title || '新的城市探索'}</span>
            <small>{formatDate(conversation.lastMessageAt)}</small>
          </button>
        ))}
        {!conversations.length && <p>还没有保存的对话。</p>}
      </div>
    </aside>
  )
}

function formatDate(value?: string) {
  if (!value) return ''
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric',
  }).format(new Date(value))
}
