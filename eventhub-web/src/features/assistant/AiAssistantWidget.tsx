import { useLocation } from 'react-router-dom'
import { AssistantLauncher } from './components/AssistantLauncher'
import { AssistantPanel } from './components/AssistantPanel'
import { useAssistant } from './useAssistant'
import './assistant.css'

export function AiAssistantWidget() {
  const location = useLocation()
  const assistant = useAssistant()

  if (['/login', '/register'].includes(location.pathname)) return null

  return (
    <div className={`ai-assistant ${assistant.open ? 'is-open' : ''}`}>
      {assistant.open ? (
        <AssistantPanel assistant={assistant} />
      ) : (
        <AssistantLauncher onOpen={assistant.openPanel} />
      )}
    </div>
  )
}
