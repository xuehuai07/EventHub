import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getActivityDetail } from '../entities/activity/api'
import {
  getFavoriteStatus,
  setFavorite,
} from '../entities/activity/favoriteApi'
import {
  deleteMyReview,
  getMyReview,
  getReviews,
  getReviewSummary,
  saveMyReview,
} from '../entities/activity/reviewApi'
import { useAuthStore } from '../shared/auth/authStore'
import '../features/activity/activity.css'
import '../features/activity/activity-detail.css'

export function ActivityDetailPage() {
  const { activityId } = useParams()
  const numericActivityId = Number(activityId)
  const user = useAuthStore((state) => state.user)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [rating, setRating] = useState<number>()
  const [reviewContent, setReviewContent] = useState<string>()
  const [feedback, setFeedback] = useState('')
  const detail = useQuery({
    queryKey: ['activity-detail', activityId],
    queryFn: () => getActivityDetail(numericActivityId),
    enabled: Boolean(activityId),
  })
  const favoriteStatus = useQuery({
    queryKey: ['activity-favorite-status', numericActivityId],
    queryFn: () => getFavoriteStatus(numericActivityId),
    enabled: Boolean(user && numericActivityId),
  })
  const reviews = useQuery({
    queryKey: ['activity-reviews', numericActivityId],
    queryFn: () => getReviews(numericActivityId),
    enabled: Boolean(numericActivityId),
  })
  const reviewSummary = useQuery({
    queryKey: ['activity-review-summary', numericActivityId, user?.id],
    queryFn: () => getReviewSummary(numericActivityId),
    enabled: Boolean(numericActivityId),
  })
  const myReview = useQuery({
    queryKey: ['my-activity-review', numericActivityId, user?.id],
    queryFn: () => getMyReview(numericActivityId),
    enabled: Boolean(user && numericActivityId),
  })

  const effectiveRating = rating ?? myReview.data?.rating ?? 5
  const effectiveReviewContent = reviewContent ?? myReview.data?.content ?? ''

  const refreshSocialData = async () => {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: ['activity-detail', activityId],
      }),
      queryClient.invalidateQueries({
        queryKey: ['activity-reviews', numericActivityId],
      }),
      queryClient.invalidateQueries({
        queryKey: ['activity-review-summary', numericActivityId],
      }),
      queryClient.invalidateQueries({
        queryKey: ['my-activity-review', numericActivityId],
      }),
    ])
  }
  const favoriteMutation = useMutation({
    mutationFn: () =>
      setFavorite(numericActivityId, favoriteStatus.data?.favorited ?? false),
    onSuccess: async () => {
      setFeedback(favoriteStatus.data?.favorited ? '已取消收藏' : '已加入收藏')
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ['activity-favorite-status', numericActivityId],
        }),
        queryClient.invalidateQueries({ queryKey: ['activity-favorites'] }),
        queryClient.invalidateQueries({
          queryKey: ['activity-detail', activityId],
        }),
      ])
    },
  })
  const reviewMutation = useMutation({
    mutationFn: () =>
      saveMyReview(numericActivityId, {
        rating: effectiveRating,
        content: effectiveReviewContent.trim(),
      }),
    onSuccess: async () => {
      setFeedback(myReview.data ? '评价已更新' : '评价已发布')
      await refreshSocialData()
    },
    onError: (error) => setFeedback(errorMessage(error, '评价提交失败')),
  })
  const deleteReviewMutation = useMutation({
    mutationFn: () => deleteMyReview(numericActivityId),
    onSuccess: async () => {
      setRating(undefined)
      setReviewContent(undefined)
      setFeedback('评价已删除')
      await refreshSocialData()
    },
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
          <div className="detail-social-line">
            <small>主办方：{activity.merchantName}</small>
            <span>
              {activity.averageRating
                ? `★ ${activity.averageRating.toFixed(1)}`
                : '暂无评分'}{' '}
              · {activity.reviewCount ?? 0} 条评价 ·{' '}
              {activity.favoriteCount ?? 0} 人收藏
            </span>
          </div>
          <button
            className={`favorite-action ${favoriteStatus.data?.favorited ? 'is-active' : ''}`}
            disabled={favoriteMutation.isPending}
            onClick={() => {
              if (!user) {
                navigate('/login', {
                  state: { from: `/activities/${numericActivityId}` },
                })
                return
              }
              favoriteMutation.mutate()
            }}
          >
            {favoriteStatus.data?.favorited ? '已收藏此活动' : '收藏此活动'}
          </button>
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
              <Link
                className="session-buy-action"
                to={`/sessions/${session.id}/tickets`}
              >
                {session.seatMode === 'FIXED' ? '立即选座' : '选择票档'}
              </Link>
            </section>
          ))}
        </aside>
      </main>
      <section className="review-section">
        <div className="review-heading">
          <div>
            <span>来自真实购票用户</span>
            <h2>现场之后，他们这样说</h2>
          </div>
          <strong>
            {reviewSummary.data?.averageRating?.toFixed(1) ?? '—'}
            <small>/ 5</small>
          </strong>
        </div>
        <div className="review-layout">
          <div className="review-list">
            {reviews.data?.items?.map((review) => (
              <article key={review.id}>
                <header>
                  <div>
                    <b>{review.userDisplayName}</b>
                    <time>{formatReviewDate(review.createdAt)}</time>
                  </div>
                  <span>{'★'.repeat(review.rating ?? 0)}</span>
                </header>
                <p>{review.content}</p>
              </article>
            ))}
            {!reviews.isPending && !reviews.data?.items?.length && (
              <div className="review-empty">
                还没有评价，第一场回忆正在发生。
              </div>
            )}
          </div>
          <aside className="review-composer">
            <span>我的评价</span>
            {!user ? (
              <>
                <h3>登录后记录你的现场感受</h3>
                <button onClick={() => navigate('/login')}>
                  登录 EventHub
                </button>
              </>
            ) : reviewSummary.data?.eligible || myReview.data ? (
              <>
                <h3>{myReview.data ? '更新我的评价' : '为这场活动评分'}</h3>
                <div className="rating-picker" aria-label="评分">
                  {[1, 2, 3, 4, 5].map((value) => (
                    <button
                      className={value <= effectiveRating ? 'is-active' : ''}
                      key={value}
                      onClick={() => setRating(value)}
                      aria-label={`${value} 分`}
                    >
                      ★
                    </button>
                  ))}
                </div>
                <textarea
                  value={effectiveReviewContent}
                  maxLength={1000}
                  placeholder="写下场馆、演出或同行人的记忆..."
                  onChange={(event) => setReviewContent(event.target.value)}
                />
                <div className="review-actions">
                  <button
                    disabled={
                      reviewMutation.isPending || !effectiveReviewContent.trim()
                    }
                    onClick={() => reviewMutation.mutate()}
                  >
                    {myReview.data ? '保存修改' : '发布评价'}
                  </button>
                  {myReview.data && (
                    <button
                      className="review-delete"
                      disabled={deleteReviewMutation.isPending}
                      onClick={() => deleteReviewMutation.mutate()}
                    >
                      删除
                    </button>
                  )}
                </div>
              </>
            ) : (
              <>
                <h3>到场之后再来评价</h3>
                <p>购买本活动门票且对应场次开始后，即可分享真实体验。</p>
                <Link to="/orders">查看我的订单</Link>
              </>
            )}
            {feedback && <small className="review-feedback">{feedback}</small>}
          </aside>
        </div>
      </section>
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

function formatReviewDate(value?: string) {
  if (!value) return ''
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value))
}

function errorMessage(error: unknown, fallback: string) {
  return (
    (
      error as {
        response?: { data?: { message?: string } }
      }
    ).response?.data?.message ?? fallback
  )
}
