interface AssistantLauncherProps {
  onOpen: () => void
}

export function AssistantLauncher({ onOpen }: AssistantLauncherProps) {
  return (
    <button
      className="ai-launcher"
      type="button"
      onClick={onOpen}
      aria-label="打开 AI 助手"
      aria-expanded={false}
    >
      <span className="ai-launcher-mark">E</span>
      <span className="ai-launcher-copy">
        <small>城市灵感</small>
        <strong>问问 AI 助手</strong>
      </span>
      <span className="ai-launcher-arrow">↗</span>
    </button>
  )
}
