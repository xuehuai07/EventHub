import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { getActivities, getActivityCategories } from '../entities/activity/api'
import { ActivityCard } from '../entities/activity/ActivityCard'
import '../features/activity/activity.css'

export function ActivityListPage() {
  const [keyword, setKeyword] = useState('')
  const [categoryId, setCategoryId] = useState<number>()
  const [city, setCity] = useState('')
  const categories = useQuery({
    queryKey: ['activity-categories'],
    queryFn: getActivityCategories,
  })
  const activities = useQuery({
    queryKey: ['activities', categoryId, city, keyword],
    queryFn: () =>
      getActivities({
        categoryId,
        city: city || undefined,
        keyword: keyword || undefined,
      }),
  })

  return (
    <div className="activity-explorer">
      <header className="activity-nav">
        <Link className="brand" to="/">
          <span className="brand-mark">E</span>
          EventHub
        </Link>
        <Link to="/">返回首页</Link>
      </header>
      <section className="explorer-heading">
        <span>城市活动日历</span>
        <h1>今天，去现场。</h1>
        <p>浏览已经过平台审核的演出、展览、讲座与城市社区活动。</p>
      </section>
      <section className="activity-filters">
        <input
          value={keyword}
          placeholder="搜索活动名称或介绍"
          onChange={(event) => setKeyword(event.target.value)}
        />
        <input
          value={city}
          placeholder="城市，例如：上海"
          onChange={(event) => setCity(event.target.value)}
        />
        <div className="category-tabs">
          <button
            className={!categoryId ? 'is-active' : ''}
            onClick={() => setCategoryId(undefined)}
          >
            全部
          </button>
          {categories.data?.map((category) => (
            <button
              key={category.id}
              className={categoryId === category.id ? 'is-active' : ''}
              onClick={() => setCategoryId(category.id)}
            >
              {category.name}
            </button>
          ))}
        </div>
      </section>
      <main className="activity-grid">
        {activities.data?.items?.map((activity) => (
          <ActivityCard key={activity.id} activity={activity} />
        ))}
        {!activities.isPending && activities.data?.total === 0 && (
          <div className="activity-empty">
            <strong>暂时没有符合条件的活动</strong>
            <p>换一个分类、城市或关键词再试试。</p>
          </div>
        )}
      </main>
    </div>
  )
}
