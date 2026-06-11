import { useMutation, useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { createSeatLock, getAvailability } from '../entities/order/api'
import { useAuthStore } from '../shared/auth/authStore'
import '../features/order/ticketing.css'

export function SeatSelectionPage() {
  const { sessionId } = useParams()
  const navigate = useNavigate()
  const authStatus = useAuthStore((state) => state.status)
  const [selectedSeats, setSelectedSeats] = useState<number[]>([])
  const [ticketId, setTicketId] = useState<number>()
  const [quantity, setQuantity] = useState(1)
  const availability = useQuery({
    queryKey: ['availability', sessionId],
    queryFn: () => getAvailability(Number(sessionId)),
    enabled: Boolean(sessionId),
    refetchInterval: 5000,
  })
  const data = availability.data
  const selectedTicketId = useMemo(() => {
    const seat = data?.seats?.find((item) =>
      selectedSeats.includes(item.id ?? 0),
    )
    return seat?.ticketTypeId
  }, [data?.seats, selectedSeats])
  const lock = useMutation({
    mutationFn: createSeatLock,
    onSuccess: (result) => {
      if (result?.lockNo) navigate(`/checkout/${result.lockNo}`)
    },
  })

  if (availability.isPending) {
    return <div className="ticketing-state">正在打开售票图...</div>
  }
  if (!data || availability.isError) {
    return <div className="ticketing-state">当前场次暂不可售</div>
  }

  const submit = () => {
    if (authStatus !== 'authenticated') {
      navigate('/login', { state: { from: `/sessions/${sessionId}/tickets` } })
      return
    }
    const selectedTicket = selectedTicketId ?? ticketId
    if (!selectedTicket) return
    lock.mutate({
      sessionId: Number(sessionId),
      ticketTypeId: selectedTicket,
      sessionSeatIds: selectedSeats,
      quantity: data.seatMode === 'FIXED' ? selectedSeats.length : quantity,
    })
  }

  return (
    <div className="ticketing-page">
      <header className="ticketing-header">
        <Link className="brand" to="/">
          <span className="brand-mark">E</span> EventHub
        </Link>
        <Link to="/orders">我的订单</Link>
      </header>
      <main className="ticketing-shell">
        <section className="ticketing-intro">
          <span>实时售票</span>
          <h1>{data.activityTitle}</h1>
          <p>
            {data.sessionName} · {data.venueName}
          </p>
          <div className="availability-note">
            页面每 5 秒同步一次库存，选中不代表锁定，以提交结果为准。
          </div>
        </section>

        {data.seatMode === 'FIXED' ? (
          <section className="seat-map-panel">
            <div className="stage-line">舞台 / STAGE</div>
            <div className="seat-legend">
              <span>可选</span>
              <span>已选</span>
              <span>锁定或售出</span>
            </div>
            <div className="seat-grid">
              {data.seats?.map((seat) => {
                const id = seat.id ?? 0
                const selected = selectedSeats.includes(id)
                const disabled =
                  seat.status !== 'AVAILABLE' ||
                  (selectedTicketId !== undefined &&
                    seat.ticketTypeId !== selectedTicketId &&
                    !selected)
                return (
                  <button
                    className={selected ? 'seat is-selected' : 'seat'}
                    disabled={disabled}
                    key={id}
                    title={`${seat.areaName} ${seat.rowLabel}排${seat.seatNumber}座`}
                    onClick={() =>
                      setSelectedSeats((current) =>
                        current.includes(id)
                          ? current.filter((item) => item !== id)
                          : [...current, id].slice(0, 6),
                      )
                    }
                  >
                    {seat.seatNumber}
                  </button>
                )
              })}
            </div>
          </section>
        ) : (
          <section className="ticket-choice-list">
            {data.ticketTypes?.map((ticket) => (
              <button
                className={
                  ticketId === ticket.id
                    ? 'ticket-choice is-selected'
                    : 'ticket-choice'
                }
                key={ticket.id}
                onClick={() => setTicketId(ticket.id)}
              >
                <span>
                  <b>{ticket.name}</b>
                  <small>剩余 {ticket.lockableStock} 张</small>
                </span>
                <strong>¥{money(ticket.priceCents)}</strong>
              </button>
            ))}
            <label className="quantity-control">
              购买数量
              <input
                min="1"
                max="6"
                type="number"
                value={quantity}
                onChange={(event) => setQuantity(Number(event.target.value))}
              />
            </label>
          </section>
        )}

        <aside className="ticketing-summary">
          <span>本次选择</span>
          <strong>
            {data.seatMode === 'FIXED'
              ? `${selectedSeats.length} 个座位`
              : `${quantity} 张票`}
          </strong>
          <button
            disabled={
              lock.isPending ||
              (data.seatMode === 'FIXED'
                ? selectedSeats.length === 0
                : !ticketId)
            }
            onClick={submit}
          >
            {lock.isPending ? '正在锁定...' : '锁定并继续'}
          </button>
          {lock.isError && (
            <p className="ticketing-error">
              锁定失败，库存可能已变化，请重试。
            </p>
          )}
        </aside>
      </main>
    </div>
  )
}

function money(value?: number) {
  return ((value ?? 0) / 100).toFixed(2)
}
