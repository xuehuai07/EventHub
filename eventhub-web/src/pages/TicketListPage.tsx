import { useQuery } from '@tanstack/react-query'
import type { CSSProperties } from 'react'
import { Link } from 'react-router-dom'
import { getTickets } from '../entities/ticket/api'
import '../features/ticket/ticket-wallet.css'

export function TicketListPage() {
  const tickets = useQuery({
    queryKey: ['tickets'],
    queryFn: () => getTickets(),
  })

  return (
    <div className="wallet-page">
      <header className="wallet-header">
        <Link className="brand" to="/">
          <span className="brand-mark">E</span> EventHub
        </Link>
        <nav>
          <Link to="/orders">我的订单</Link>
          <Link to="/notifications">通知</Link>
        </nav>
      </header>
      <main className="wallet-shell">
        <div className="wallet-heading">
          <span>DIGITAL WALLET</span>
          <h1>我的票券</h1>
          <p>每一张票都是一段即将发生的现场。</p>
        </div>
        <div className="ticket-stack">
          {tickets.data?.items?.map((ticket, index) => (
            <Link
              className={`wallet-ticket status-${ticket.status?.toLowerCase()}`}
              style={{ '--ticket-index': index } as CSSProperties}
              to={`/tickets/${ticket.id}`}
              key={ticket.id}
            >
              <div className="ticket-date">
                <strong>{datePart(ticket.startAt, 'day')}</strong>
                <span>{datePart(ticket.startAt, 'month')}</span>
              </div>
              <div className="ticket-copy">
                <small>{ticket.venueName}</small>
                <h2>{ticket.activityTitle}</h2>
                <p>
                  {ticket.sessionName} · {ticket.ticketTypeName}
                </p>
              </div>
              <div className="ticket-state">{statusLabel(ticket.status)}</div>
            </Link>
          ))}
          {!tickets.isPending && !tickets.data?.items?.length && (
            <div className="wallet-empty">支付成功后，电子票会出现在这里。</div>
          )}
        </div>
      </main>
    </div>
  )
}

function datePart(value: string | undefined, part: 'day' | 'month') {
  if (!value) return '--'
  const date = new Date(value)
  return part === 'day'
    ? String(date.getDate()).padStart(2, '0')
    : `${date.getMonth() + 1}月`
}

function statusLabel(status?: string) {
  return (
    { UNUSED: '待使用', USED: '已核销', CANCELLED: '已作废' }[status ?? ''] ??
    '未知'
  )
}
