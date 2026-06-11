import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  createOrder,
  getSeatLock,
  releaseSeatLock,
} from '../entities/order/api'
import { SeatLockCountdown } from '../features/seat-lock-countdown/SeatLockCountdown'
import '../features/order/ticketing.css'

export function OrderConfirmationPage() {
  const { lockNo = '' } = useParams()
  const navigate = useNavigate()
  const [expired, setExpired] = useState(false)
  const seatLock = useQuery({
    queryKey: ['seat-lock', lockNo],
    queryFn: () => getSeatLock(lockNo),
    enabled: Boolean(lockNo),
  })
  const order = useMutation({
    mutationFn: () => createOrder({ lockNo }),
    onSuccess: (result) => result?.id && navigate(`/orders/${result.id}`),
  })
  const abandon = useMutation({
    mutationFn: () => releaseSeatLock(lockNo),
    onSuccess: () => navigate('/activities'),
  })
  const data = seatLock.data

  if (seatLock.isPending)
    return <div className="ticketing-state">正在确认锁定...</div>
  if (!data || seatLock.isError)
    return <div className="ticketing-state">锁定不存在或已失效</div>

  return (
    <main className="checkout-page">
      <Link className="brand" to="/">
        <span className="brand-mark">E</span> EventHub
      </Link>
      <section className="checkout-card">
        <div className="checkout-kicker">座位已为你保留</div>
        <h1>确认订单</h1>
        <div className="checkout-countdown">
          <span>请在倒计时结束前提交</span>
          <SeatLockCountdown
            expiresAt={data.expiresAt}
            onExpired={() => setExpired(true)}
          />
        </div>
        <dl>
          <div>
            <dt>场次编号</dt>
            <dd>{data.sessionId}</dd>
          </div>
          <div>
            <dt>票数</dt>
            <dd>{data.quantity} 张</dd>
          </div>
          <div>
            <dt>座位类型</dt>
            <dd>{data.seatMode === 'FIXED' ? '固定座位' : '自由入场'}</dd>
          </div>
          <div>
            <dt>应付金额</dt>
            <dd className="checkout-total">
              ¥{((data.amountCents ?? 0) / 100).toFixed(2)}
            </dd>
          </div>
        </dl>
        <button
          disabled={expired || order.isPending}
          onClick={() => order.mutate()}
        >
          {expired
            ? '锁定已过期'
            : order.isPending
              ? '正在创建订单...'
              : '提交订单'}
        </button>
        <button className="checkout-secondary" onClick={() => abandon.mutate()}>
          放弃本次选择
        </button>
        {order.isError && (
          <p className="ticketing-error">订单创建失败，请返回重新选择。</p>
        )}
      </section>
    </main>
  )
}
