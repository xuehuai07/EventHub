import type { AssistantController } from '../useAssistant'
import { AssistantComposer } from './AssistantComposer'
import { AssistantHistory } from './AssistantHistory'
import { AssistantMessages } from './AssistantMessages'

export function AssistantPanel({
  assistant,
}: {
  assistant: AssistantController
}) {
  return (
    <section
      className="ai-panel"
      aria-label="EventHub AI 智能助手"
      role="dialog"
    >
      <header className="ai-panel-header">
        <button
          className="ai-icon-button"
          type="button"
          onClick={() => assistant.setHistoryOpen(!assistant.historyOpen)}
          aria-label="对话历史"
          disabled={!assistant.authenticated}
        >
          <MenuIcon />
        </button>
        <div className="ai-title">
          <span>EventHub</span>
          <strong>城市灵感助手</strong>
        </div>
        <button
          className="ai-icon-button"
          type="button"
          onClick={assistant.closePanel}
          aria-label="关闭助手"
        >
          <CloseIcon />
        </button>
      </header>

      {assistant.historyOpen && (
        <AssistantHistory
          conversations={assistant.conversations}
          activeId={assistant.activeId}
          onNew={assistant.startNewConversation}
          onSelect={(id) => void assistant.selectConversation(id)}
        />
      )}

      {assistant.authenticated ? (
        <div className="ai-toolbar">
          <button type="button" onClick={assistant.startNewConversation}>
            新对话
          </button>
          <span />
          <button
            type="button"
            onClick={() => void assistant.renameActive()}
            disabled={!assistant.activeId}
          >
            重命名
          </button>
          <button
            type="button"
            onClick={() => void assistant.removeActive()}
            disabled={!assistant.activeId || assistant.generating}
          >
            删除
          </button>
        </div>
      ) : (
        <div className="ai-guest-bar">
          <span>登录后可保存对话并查询本人订单与票券</span>
          <button type="button" onClick={assistant.goToLogin}>
            去登录
          </button>
        </div>
      )}

      <AssistantMessages
        messages={assistant.messages}
        loading={assistant.loading}
        scrollRef={assistant.scrollRef}
        onPrompt={(prompt) => void assistant.send(prompt)}
        onResource={assistant.openResource}
      />

      {assistant.error && (
        <div className="ai-error">
          <span>{assistant.error}</span>
          {assistant.lastPrompt && !assistant.generating && (
            <button
              type="button"
              onClick={() => void assistant.send(assistant.lastPrompt || '')}
            >
              重新发送
            </button>
          )}
        </div>
      )}

      <AssistantComposer
        input={assistant.input}
        generating={assistant.generating}
        authenticated={assistant.authenticated}
        onInput={assistant.setInput}
        onSend={() => void assistant.send()}
        onStop={assistant.stop}
        onLogin={assistant.goToLogin}
      />
    </section>
  )
}

function MenuIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 6h16M4 12h12M4 18h8" />
    </svg>
  )
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="m6 6 12 12M18 6 6 18" />
    </svg>
  )
}
