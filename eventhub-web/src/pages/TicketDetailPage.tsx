import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { getTicket } from '../entities/ticket/api'
import { TicketQr } from '../features/ticket/TicketQr'
import '../features/ticket/ticket-wallet.css'

export function TicketDetailPage() {
  const { ticketId } = useParams()
  const ticket = useQuery({
    queryKey: ['ticket', ticketId],
    queryFn: () => getTicket(Number(ticketId)),
    enabled: Boolean(ticketId),
  })
  const data = ticket.data
  if (ticket.isPending)
    return <div className="ticketing-state">正在打开电子票...</div>
  if (!data) return <div className="ticketing-state">票券不存在</div>

  return (
    <main className="ticket-detail-page">
      <Link className="brand" to="/tickets">
        <span className="brand-mark">E</span> 返回票夹
      </Link>
      <section className="admission-pass">
        <div
          className="pass-visual"
          style={{ backgroundImage: `url(${data.coverUrl || ''})` }}
        >
          <span>
            {data.status === 'UNUSED' ? 'ADMIT ONE' : statusLabel(data.status)}
          </span>
          <h1>{data.activityTitle}</h1>
          <p>{data.sessionName}</p>
        </div>
        <div className="pass-body">
          <div className="pass-meta">
            <div>
              <span>时间</span>
              <strong>{formatTime(data.startAt)}</strong>
            </div>
            <div>
              <span>场馆</span>
              <strong>{data.venueName}</strong>
            </div>
            <div>
              <span>票档</span>
              <strong>{data.ticketTypeName}</strong>
            </div>
            <div>
              <span>座位</span>
              <strong>{seat(data)}</strong>
            </div>
          </div>
          {data.status === 'UNUSED' ? (
            <TicketQr ticketId={data.id!} />
          ) : (
            <div className="used-stamp">
              <strong>{statusLabel(data.status)}</strong>
              {data.usedAt && (
                <span>
                  {formatTime(data.usedAt)} · {data.verifierName}
                </span>
              )}
            </div>
          )}
          <code>{data.ticketNo}</code>
        </div>
      </section>
    </main>
  )
}

function statusLabel(status?: string) {
  return (
    { UNUSED: '待使用', USED: '已核销', CANCELLED: '已作废' }[status ?? ''] ??
    '未知'
  )
}
function formatTime(value?: string) {
  return value
    ? new Date(value).toLocaleString('zh-CN', { hour12: false })
    : '--'
}
function seat(ticket: {
  areaName?: string
  rowLabel?: string
  seatNumber?: string
}) {
  return ticket.areaName
    ? `${ticket.areaName} ${ticket.rowLabel}排 ${ticket.seatNumber}座`
    : '无固定座位'
}
