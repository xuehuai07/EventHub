import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  getNotifications,
  readAllNotifications,
  readNotification,
} from '../entities/notification/api'
import '../features/ticket/ticket-wallet.css'

export function NotificationPage() {
  const queryClient = useQueryClient()
  const notifications = useQuery({
    queryKey: ['notifications'],
    queryFn: getNotifications,
  })
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['notifications'] })
    await queryClient.invalidateQueries({ queryKey: ['unread-count'] })
  }
  const readAll = useMutation({
    mutationFn: readAllNotifications,
    onSuccess: refresh,
  })

  return (
    <main className="notification-page">
      <div className="notification-toolbar">
        <Link className="brand" to="/">
          <span className="brand-mark">E</span> EventHub
        </Link>
        <button onClick={() => readAll.mutate()}>全部标为已读</button>
      </div>
      <header>
        <span>INBOX</span>
        <h1>站内通知</h1>
      </header>
      <div className="notification-list">
        {notifications.data?.items?.map((item) => (
          <button
            className={item.readAt ? 'is-read' : ''}
            key={item.id}
            onClick={() => item.id && readNotification(item.id).then(refresh)}
          >
            <span>{item.readAt ? '' : 'NEW'}</span>
            <div>
              <strong>{item.title}</strong>
              <p>{item.content}</p>
            </div>
            <time>
              {item.createdAt
                ? new Date(item.createdAt).toLocaleString('zh-CN')
                : ''}
            </time>
          </button>
        ))}
      </div>
    </main>
  )
}
