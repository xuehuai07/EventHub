import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getOrders } from '../entities/order/api'
import '../features/order/ticketing.css'

const statusText = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  CANCELLED: '已取消',
  EXPIRED: '已过期',
}

export function OrderListPage() {
  const orders = useQuery({ queryKey: ['orders'], queryFn: getOrders })

  return (
    <div className="orders-page">
      <header className="ticketing-header">
        <Link className="brand" to="/">
          <span className="brand-mark">E</span> EventHub
        </Link>
        <Link to="/activities">继续发现活动</Link>
      </header>
      <main className="orders-shell">
        <div className="orders-heading">
          <span>个人中心</span>
          <h1>我的订单</h1>
          <p>记录每一次出发，也保留尚未完成的选择。</p>
        </div>
        <div className="order-list">
          {orders.data?.items?.map((order) => (
            <Link
              className="order-row"
              to={`/orders/${order.id}`}
              key={order.id}
            >
              <div>
                <small>{order.orderNo}</small>
                <strong>{order.activityTitle}</strong>
                <span>
                  {order.sessionName} · {order.venueName}
                </span>
              </div>
              <div>
                <b
                  className={`order-status status-${order.status?.toLowerCase()}`}
                >
                  {order.status ? statusText[order.status] : '未知'}
                </b>
                <strong>
                  ¥{((order.totalAmountCents ?? 0) / 100).toFixed(2)}
                </strong>
              </div>
            </Link>
          ))}
          {!orders.isPending && !orders.data?.items?.length && (
            <div className="empty-orders">
              还没有订单，从一场感兴趣的活动开始吧。
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
