import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getSystemStatus } from '../shared/api/system'

const features = [
  {
    number: '01',
    title: '发现身边好活动',
    body: '按日期、地点和活动类型浏览经过平台审核的城市活动。',
  },
  {
    number: '02',
    title: '在线选座与锁座',
    body: '直观选择心仪座位，并在明确的倒计时内完成订单确认。',
  },
  {
    number: '03',
    title: '电子票便捷入场',
    body: '订单与电子票统一管理，从购票到现场核销全程清晰可查。',
  },
]

export function HomePage() {
  const systemQuery = useQuery({
    queryKey: ['system-status'],
    queryFn: getSystemStatus,
  })
  const isUp = systemQuery.data?.data.status === 'UP'

  return (
    <div className="site-shell">
      <header className="site-header">
        <Link className="brand" to="/">
          <span className="brand-mark">E</span>
          EventHub
        </Link>
        <nav className="nav-links" aria-label="主导航">
          <a href="#discover">发现活动</a>
          <a href="#how-it-works">使用方式</a>
          <a href="#system">系统状态</a>
        </nav>
        <a className="header-action" href="#discover">
          浏览活动
        </a>
      </header>

      <main>
        <section className="hero-section" id="discover">
          <div className="hero-copy">
            <span className="eyebrow">探索城市生活</span>
            <h1>
              发现城市里的<em>每一种精彩</em>
            </h1>
            <p>
              汇集演出、展览、讲座和城市社区活动，从发现、选座到持票入场，
              为你提供清晰顺畅的一站式活动体验。
            </p>
            <div className="hero-actions">
              <a className="primary-action" href="#how-it-works">
                开始探索
              </a>
              <a className="secondary-action" href="#system">
                查看平台状态
              </a>
            </div>
          </div>

          <aside className="city-card" aria-label="精选活动预览">
            <div className="date-chip">
              <strong>21</strong>
              <span>九月</span>
            </div>
            <div className="event-preview">
              <small>本周精选</small>
              <h2>城市夜游计划</h2>
              <p>在老仓库街区体验音乐、灯光与独立文化交织的城市夜晚。</p>
            </div>
          </aside>
        </section>

        <section
          className="feature-strip"
          id="how-it-works"
          aria-label="平台功能"
        >
          {features.map((feature) => (
            <article className="feature-item" key={feature.number}>
              <span className="feature-number">{feature.number}</span>
              <h3>{feature.title}</h3>
              <p>{feature.body}</p>
            </article>
          ))}
        </section>

        <section className="system-bar" id="system">
          <div>
            <strong>EventHub 服务连接状态</strong>
            <p>
              {systemQuery.isPending
                ? '正在通过开发代理检查后端服务。'
                : systemQuery.isError
                  ? '前端已就绪，但后端服务暂未响应。'
                  : `${systemQuery.data.data.service} 已响应，请求编号：${systemQuery.data.requestId}。`}
            </p>
          </div>
          <span
            className={`status-pill ${isUp ? 'is-up' : systemQuery.isError ? 'is-down' : ''}`}
          >
            <span className="status-dot" aria-hidden="true" />
            {systemQuery.isPending
              ? '检查中'
              : isUp
                ? '服务正常'
                : '服务不可用'}
          </span>
        </section>
      </main>
    </div>
  )
}
