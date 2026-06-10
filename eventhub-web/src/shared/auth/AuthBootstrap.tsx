import { useEffect, type PropsWithChildren } from 'react'
import { refreshAccessToken } from '../api/http'
import { useAuthStore } from './authStore'

export function AuthBootstrap({ children }: PropsWithChildren) {
  const status = useAuthStore((state) => state.status)
  const clearSession = useAuthStore((state) => state.clearSession)

  useEffect(() => {
    refreshAccessToken().catch(clearSession)
  }, [clearSession])

  if (status === 'bootstrapping') {
    return <div className="app-loading">正在恢复登录状态...</div>
  }

  return children
}
