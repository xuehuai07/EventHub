import {
  ArrowUpOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  DollarOutlined,
  ShopOutlined,
  TagsOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Spin } from 'antd'
import {
  getOperationsDashboard,
  getSalesTrend,
  getTopActivities,
} from '../entities/operations/api'
import { useAuthStore } from '../shared/auth/authStore'

export function DashboardPage() {
  const user = useAuthStore((state) => state.user)
  const isAdmin = user?.roles?.includes('ADMIN') ?? false
  const metrics = useQuery({
    queryKey: ['operations-dashboard', isAdmin],
    queryFn: () => getOperationsDashboard(isAdmin),
  })
  const trend = useQuery({
    queryKey: ['sales-trend', isAdmin],
    queryFn: () => getSalesTrend(isAdmin),
  })
  const topActivities = useQuery({
    queryKey: ['top-activities', isAdmin],
    queryFn: () => getTopActivities(isAdmin),
  })
  const maxTrend = Math.max(
    1,
    ...(trend.data ?? []).map((item) => item.paidAmountCents ?? 0),
  )

  return (
    <div className="operations-dashboard">
      <section className="operations-hero">
        <div>
          <span className="section-label">0.7.0 · 运营视图</span>
          <h1>{isAdmin ? '平台正在发生什么' : '今天的生意，清楚可见'}</h1>
          <p>
            {isAdmin
              ? '从成交、履约和活动热度观察 EventHub 的真实运行状态。'
              : '所有指标仅统计当前商家的已支付订单与已生成票券。'}
          </p>
        </div>
        <div className="operations-scope">
          <span>{isAdmin ? 'PLATFORM' : 'MERCHANT'}</span>
          <strong>{formatToday()}</strong>
          <small>数据以 MySQL 业务事实为准</small>
        </div>
      </section>

      {metrics.isPending ? (
        <div className="dashboard-loading">
          <Spin />
        </div>
      ) : (
        <section className="operations-metrics">
          <Metric
            icon={<DollarOutlined />}
            label="累计成交额"
            value={formatMoney(metrics.data?.paidAmountCents)}
          />
          <Metric
            icon={<ArrowUpOutlined />}
            label="已支付订单"
            value={`${metrics.data?.paidOrderCount ?? 0} 笔`}
          />
          <Metric
            icon={<TagsOutlined />}
            label="售出票数"
            value={`${metrics.data?.soldTicketCount ?? 0} 张`}
          />
          <Metric
            icon={<CheckCircleOutlined />}
            label="已核销"
            value={`${metrics.data?.usedTicketCount ?? 0} 张`}
          />
          <Metric
            icon={<CalendarOutlined />}
            label="已发布活动"
            value={`${metrics.data?.publishedActivityCount ?? 0} 个`}
          />
          <Metric
            icon={<ShopOutlined />}
            label={isAdmin ? '活跃商家' : '统计范围'}
            value={
              isAdmin
                ? `${metrics.data?.activeMerchantCount ?? 0} 家`
                : '当前商家'
            }
          />
        </section>
      )}

      <section className="operations-grid">
        <article className="trend-panel">
          <div className="operations-heading">
            <div>
              <span className="section-label">最近 30 天</span>
              <h2>成交趋势</h2>
            </div>
            <small>按支付日期聚合</small>
          </div>
          <div className="trend-chart">
            {(trend.data ?? []).map((item) => (
              <div className="trend-column" key={item.date}>
                <span
                  style={{
                    height: `${Math.max(6, ((item.paidAmountCents ?? 0) / maxTrend) * 100)}%`,
                  }}
                  title={`${item.date} · ${formatMoney(item.paidAmountCents)}`}
                />
                <small>{item.date?.slice(5)}</small>
              </div>
            ))}
            {!trend.isPending && !trend.data?.length && (
              <div className="operations-empty">最近 30 天暂无成交数据</div>
            )}
          </div>
        </article>

        <article className="ranking-panel">
          <div className="operations-heading">
            <div>
              <span className="section-label">TOP ACTIVITIES</span>
              <h2>热门活动</h2>
            </div>
          </div>
          <div className="activity-ranking">
            {(topActivities.data ?? []).map((activity, index) => (
              <div key={activity.activityId}>
                <b>{String(index + 1).padStart(2, '0')}</b>
                <span>
                  <strong>{activity.title}</strong>
                  <small>{activity.soldTicketCount ?? 0} 张票</small>
                </span>
                <em>{formatMoney(activity.paidAmountCents)}</em>
              </div>
            ))}
            {!topActivities.isPending && !topActivities.data?.length && (
              <div className="operations-empty">暂无热门活动数据</div>
            )}
          </div>
        </article>
      </section>
    </div>
  )
}

function Metric({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode
  label: string
  value: string
}) {
  return (
    <article>
      <span>{icon}</span>
      <small>{label}</small>
      <strong>{value}</strong>
    </article>
  )
}

function formatMoney(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
  }).format((value ?? 0) / 100)
}

function formatToday() {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(new Date())
}
