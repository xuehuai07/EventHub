import { useQuery } from '@tanstack/react-query'
import { Link, Navigate } from 'react-router-dom'
import { getFavorites } from '../entities/activity/favoriteApi'
import { useAuthStore } from '../shared/auth/authStore'
import '../features/activity/activity.css'
import '../features/activity/favorites.css'

export function FavoriteListPage() {
  const user = useAuthStore((state) => state.user)
  const favorites = useQuery({
    queryKey: ['activity-favorites'],
    queryFn: () => getFavorites(),
    enabled: Boolean(user),
  })

  if (!user) {
    return <Navigate to="/login" replace state={{ from: '/favorites' }} />
  }

  return (
    <div className="favorites-page">
      <header className="activity-nav">
        <Link className="brand" to="/">
          <span className="brand-mark">E</span>
          EventHub
        </Link>
        <Link to="/activities">继续发现</Link>
      </header>
      <section className="favorites-heading">
        <span>MY CITY NOTES</span>
        <h1>收藏的现场</h1>
        <p>把想去的活动留在这里，等一个合适的日期，也等一个同行的人。</p>
      </section>
      <main className="favorites-grid">
        {favorites.data?.items?.map((activity, index) => (
          <Link
            className="favorite-card"
            style={{ '--card-index': index } as React.CSSProperties}
            key={activity.activityId}
            to={`/activities/${activity.activityId}`}
          >
            <div
              className="favorite-cover"
              style={
                activity.coverUrl
                  ? { backgroundImage: `url("${activity.coverUrl}")` }
                  : undefined
              }
            >
              <span>{activity.categoryName}</span>
              {activity.status !== 'PUBLISHED' && <b>当前不可售</b>}
            </div>
            <div className="favorite-copy">
              <small>
                {activity.city} ·{' '}
                {activity.nextSessionAt
                  ? formatDate(activity.nextSessionAt)
                  : '暂无未来场次'}
              </small>
              <h2>{activity.title}</h2>
              <p>{activity.summary}</p>
              <footer>
                <span>
                  {activity.minimumPriceCents == null
                    ? '票价待定'
                    : `¥${(activity.minimumPriceCents / 100).toFixed(0)} 起`}
                </span>
                <i>查看活动</i>
              </footer>
            </div>
          </Link>
        ))}
        {!favorites.isPending && !favorites.data?.items?.length && (
          <section className="favorites-empty">
            <span>还没有留下标记</span>
            <h2>下一场城市记忆，从收藏开始。</h2>
            <Link to="/activities">浏览活动</Link>
          </section>
        )}
      </main>
    </div>
  )
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'short',
  }).format(new Date(value))
}
