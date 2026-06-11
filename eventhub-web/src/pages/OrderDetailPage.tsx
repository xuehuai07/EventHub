import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { cancelOrder, getOrder, payOrder } from '../entities/order/api'
import { SeatLockCountdown } from '../features/seat-lock-countdown/SeatLockCountdown'
import '../features/order/ticketing.css'

export function OrderDetailPage() {
  const { orderId } = useParams()
  const queryClient = useQueryClient()
  const order = useQuery({
    queryKey: ['order', orderId],
    queryFn: () => getOrder(Number(orderId)),
    enabled: Boolean(orderId),
  })
  const refresh = () =>
    queryClient.invalidateQueries({ queryKey: ['order', orderId] })
  const payment = useMutation({
    mutationFn: () => payOrder(Number(orderId)),
    onSuccess: refresh,
  })
  const cancellation = useMutation({
    mutationFn: () => cancelOrder(Number(orderId)),
    onSuccess: refresh,
  })
  const data = order.data

  if (order.isPending)
    return <div className="ticketing-state">正在读取订单...</div>
  if (!data) return <div className="ticketing-state">订单不存在</div>

  return (
    <main className="order-detail-page">
      <Link className="brand" to="/orders">
        <span className="brand-mark">E</span> 返回我的订单
      </Link>
      <section className="order-receipt">
        <div className="receipt-head">
          <div>
            <span>订单 {data.orderNo}</span>
            <h1>{data.activityTitle}</h1>
            <p>
              {data.sessionName} · {data.venueName}
            </p>
          </div>
          <b>{statusLabel(data.status)}</b>
        </div>
        {data.status === 'PENDING_PAYMENT' && (
          <div className="payment-deadline">
            <span>剩余支付时间</span>
            <SeatLockCountdown expiresAt={data.paymentDeadlineAt} />
          </div>
        )}
        <div className="receipt-items">
          {data.items?.map((item) => (
            <div key={item.id}>
              <span>
                <strong>{item.ticketTypeName}</strong>
                <small>
                  {item.areaName
                    ? `${item.areaName} ${item.rowLabel}排 ${item.seatNumber}座`
                    : `${item.quantity} 张`}
                </small>
              </span>
              <b>¥{((item.subtotalCents ?? 0) / 100).toFixed(2)}</b>
            </div>
          ))}
        </div>
        <div className="receipt-total">
          <span>订单合计</span>
          <strong>¥{((data.totalAmountCents ?? 0) / 100).toFixed(2)}</strong>
        </div>
        {data.status === 'PENDING_PAYMENT' && (
          <div className="order-actions">
            <button
              disabled={payment.isPending}
              onClick={() => payment.mutate()}
            >
              {payment.isPending ? '支付处理中...' : '模拟支付'}
            </button>
            <button
              className="checkout-secondary"
              disabled={cancellation.isPending}
              onClick={() => cancellation.mutate()}
            >
              取消订单
            </button>
          </div>
        )}
      </section>
    </main>
  )
}

function statusLabel(status?: string) {
  return (
    {
      PENDING_PAYMENT: '待支付',
      PAID: '已支付',
      CANCELLED: '已取消',
      EXPIRED: '已过期',
    }[status ?? ''] ?? '未知状态'
  )
}
