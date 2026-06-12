import { Client } from '@stomp/stompjs'
import { useQueryClient } from '@tanstack/react-query'
import { useEffect, type PropsWithChildren } from 'react'
import { useAuthStore } from '../auth/authStore'

export function RealtimeBridge({ children }: PropsWithChildren) {
  const queryClient = useQueryClient()
  const accessToken = useAuthStore((state) => state.accessToken)
  const userId = useAuthStore((state) => state.user?.id)

  useEffect(() => {
    if (!accessToken || !userId) return
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const refresh = () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] })
      void queryClient.invalidateQueries({ queryKey: ['unread-count'] })
      void queryClient.invalidateQueries({ queryKey: ['orders'] })
      void queryClient.invalidateQueries({ queryKey: ['tickets'] })
    }
    const client = new Client({
      brokerURL: `${protocol}://${window.location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        refresh()
        client.subscribe('/user/queue/notifications', refresh)
        client.subscribe('/user/queue/status', refresh)
      },
    })
    window.addEventListener('online', refresh)
    client.activate()
    return () => {
      window.removeEventListener('online', refresh)
      void client.deactivate()
    }
  }, [accessToken, queryClient, userId])

  return children
}
