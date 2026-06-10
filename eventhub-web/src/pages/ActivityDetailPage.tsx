import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { getActivityDetail } from '../entities/activity/api'
import '../features/activity/activity.css'
import '../features/activity/activity-detail.css'

export function ActivityDetailPage() {
  const { activityId } = useParams()
  const detail = useQuery({
    queryKey: ['activity-detail', activityId],
    queryFn: () => getActivityDetail(Number(activityId)),
    enabled: Boolean(activityId),
  })
  const activity = detail.data

  if (detail.isPending) {
    return <div className="activity-state">正在读取活动...</div>
  }
  if (!activity || detail.isError) {
    return (
      <div className="activity-state">
        <strong>活动暂不可查看</strong>
        <Link to="/activities">返回活动列表</Link>
      </div>
    )
  }

  return (
    <div className="activity-detail-page">
      <header className="activity-nav">
        <Link className="brand" to="/">
          <span className="brand-mark">E</span>
          EventHub
        </Link>
        <Link to="/activities">全部活动</Link>
      </header>
      <section className="detail-hero">
        <div className="detail-copy">
          <span>
            {activity.categoryName} · {activity.city}
          </span>
          <h1>{activity.title}</h1>
          <p>{activity.summary}</p>
          <small>主办方：{activity.merchantName}</small>
        </div>
        <div
          className="detail-cover"
          style={
            activity.coverUrl
              ? { backgroundImage: `url("${activity.coverUrl}")` }
              : undefined
          }
        />
      </section>
      <main className="detail-layout">
        <article className="detail-description">
          <span>关于活动</span>
          <h2>现场不止是一次抵达</h2>
          <p>{activity.description}</p>
        </article>
        <aside className="session-panel">
          <span>选择场次</span>
          {activity.sessions?.map((session) => (
            <section className="session-card" key={session.id}>
              <div>
                <strong>{session.name}</strong>
                <time>{formatDateTime(session.startAt)}</time>
              </div>
              <p>
                {session.venueName} · {session.venueAddress}
              </p>
              <div className="ticket-list">
                {session.ticketTypes?.map((ticket) => (
                  <div key={ticket.id}>
                    <span>{ticket.name}</span>
                    <strong>
                      ¥{((ticket.priceCents ?? 0) / 100).toFixed(2)}
                    </strong>
                  </div>
                ))}
              </div>
              {session.seatMode === 'FIXED' && (
                <div className="seat-preview">
                  <b>固定座位预览</b>
                  {session.seatAreas?.map((area) => (
                    <span key={`${area.areaName}-${area.seatGrade}`}>
                      {area.areaName} · {area.seatGrade} · {area.seatCount} 席
                    </span>
                  ))}
                </div>
              )}
              <button disabled>选座购票即将开放</button>
            </section>
          ))}
        </aside>
      </main>
    </div>
  )
}

function formatDateTime(value?: string) {
  if (!value) return '时间待定'
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
