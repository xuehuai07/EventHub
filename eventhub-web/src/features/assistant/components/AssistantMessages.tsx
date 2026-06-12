import type { ResourceCard } from '../../../shared/api/generated/types.gen'
import { quickPrompts, type UiMessage } from '../types'

interface AssistantMessagesProps {
  messages: UiMessage[]
  loading: boolean
  scrollRef: React.RefObject<HTMLDivElement | null>
  onPrompt: (prompt: string) => void
  onResource: (resource: ResourceCard) => void
}

export function AssistantMessages({
  messages,
  loading,
  scrollRef,
  onPrompt,
  onResource,
}: AssistantMessagesProps) {
  return (
    <div className="ai-messages" ref={scrollRef} aria-live="polite">
      {!messages.length && !loading && <Welcome onPrompt={onPrompt} />}
      {loading && <div className="ai-loading">正在翻阅对话记录...</div>}
      {messages.map((message) => (
        <article
          className={`ai-message ai-message-${message.role?.toLowerCase()} ${
            message.failed ? 'is-failed' : ''
          }`}
          key={message.id || message.localId}
        >
          <small>{message.role === 'USER' ? '你' : 'EventHub 助手'}</small>
          <p>{message.content}</p>
          {message.pending && <span className="ai-typing">生成中</span>}
          {!!message.resources?.length && (
            <ResourceCards
              resources={message.resources}
              onResource={onResource}
            />
          )}
        </article>
      ))}
    </div>
  )
}

function Welcome({ onPrompt }: { onPrompt: (prompt: string) => void }) {
  return (
    <div className="ai-welcome">
      <div className="ai-orbit">
        <span>E</span>
      </div>
      <small>READ-ONLY CITY CONCIERGE</small>
      <h2>从一场活动开始，安排你的城市时间。</h2>
      <p>
        我可以推荐站内活动、找到可售场次，也能查询你本人已支付的订单和票券状态。
      </p>
      <div className="ai-quick-prompts">
        {quickPrompts.map((prompt, index) => (
          <button type="button" key={prompt} onClick={() => onPrompt(prompt)}>
            <b>0{index + 1}</b>
            <span>{prompt}</span>
          </button>
        ))}
      </div>
    </div>
  )
}

function ResourceCards({
  resources,
  onResource,
}: {
  resources: ResourceCard[]
  onResource: (resource: ResourceCard) => void
}) {
  return (
    <div className="ai-resources">
      {resources.map((resource) => (
        <button
          type="button"
          key={`${resource.type}-${resource.id}`}
          onClick={() => onResource(resource)}
        >
          <span className="ai-resource-index">
            {resourceLabel(resource.type)}
          </span>
          <span>
            <strong>{resource.title}</strong>
            <small>{resource.subtitle}</small>
          </span>
          <b>↗</b>
        </button>
      ))}
    </div>
  )
}

function resourceLabel(type?: string) {
  return (
    {
      ACTIVITY: '活动',
      SESSION: '场次',
      ORDER: '订单',
      TICKET: '票券',
    }[type || ''] || '查看'
  )
}
