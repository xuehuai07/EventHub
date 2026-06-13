interface AssistantComposerProps {
  input: string
  generating: boolean
  authenticated: boolean
  onInput: (value: string) => void
  onSend: () => void
  onStop: () => void
  onLogin: () => void
}

export function AssistantComposer({
  input,
  generating,
  authenticated,
  onInput,
  onSend,
  onStop,
  onLogin,
}: AssistantComposerProps) {
  if (!authenticated) {
    return (
      <div className="ai-login-gate">
        <span>登录后即可获得活动建议，并查询你的订单与票券。</span>
        <button type="button" onClick={onLogin}>
          登录后开始对话
        </button>
      </div>
    )
  }

  return (
    <form
      className="ai-composer"
      onSubmit={(event) => {
        event.preventDefault()
        onSend()
      }}
    >
      <textarea
        value={input}
        maxLength={2000}
        rows={1}
        placeholder="描述时间、城市、预算或想看的活动..."
        onChange={(event) => onInput(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault()
            onSend()
          }
        }}
      />
      {generating ? (
        <button className="ai-stop" type="button" onClick={onStop}>
          <span /> 停止
        </button>
      ) : (
        <button
          className="ai-send"
          type="submit"
          disabled={!input.trim()}
          aria-label="发送消息"
        >
          <SendIcon />
        </button>
      )}
    </form>
  )
}

function SendIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="m4 4 16 8-16 8 3-8-3-8Z" />
      <path d="M7 12h13" />
    </svg>
  )
}
