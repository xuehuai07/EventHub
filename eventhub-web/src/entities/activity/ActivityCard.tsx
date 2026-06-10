import { Link } from 'react-router-dom'
import type { ActivitySummaryView } from '../../shared/api/generated/types.gen'

export function ActivityCard({ activity }: { activity: ActivitySummaryView }) {
  return (
    <Link className="activity-card" to={`/activities/${activity.id}`}>
      <div
        className="activity-card-cover"
        style={
          activity.coverUrl
            ? { backgroundImage: `url("${activity.coverUrl}")` }
            : undefined
        }
      >
        <span>{activity.categoryName}</span>
      </div>
      <div className="activity-card-body">
        <p>
          {activity.city} ·{' '}
          {activity.nextSessionAt
            ? formatDate(activity.nextSessionAt)
            : '场次待定'}
        </p>
        <h3>{activity.title}</h3>
        <span>{activity.summary}</span>
        <footer>
          <strong>
            {activity.minimumPriceCents == null
              ? '免费或待定'
              : `¥${(activity.minimumPriceCents / 100).toFixed(0)} 起`}
          </strong>
          <i>查看详情</i>
        </footer>
      </div>
    </Link>
  )
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'short',
  }).format(new Date(value))
}
