import { useEffect, useState } from 'react'

export function SeatLockCountdown({
  expiresAt,
  onExpired,
}: {
  expiresAt?: string
  onExpired?: () => void
}) {
  const [remaining, setRemaining] = useState(() => secondsUntil(expiresAt))

  useEffect(() => {
    const timer = window.setInterval(() => {
      const next = secondsUntil(expiresAt)
      setRemaining(next)
      if (next === 0) onExpired?.()
    }, 1000)
    return () => window.clearInterval(timer)
  }, [expiresAt, onExpired])

  const minutes = Math.floor(remaining / 60)
  const seconds = remaining % 60
  return (
    <strong className={remaining < 60 ? 'countdown is-urgent' : 'countdown'}>
      {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
    </strong>
  )
}

function secondsUntil(value?: string) {
  if (!value) return 0
  return Math.max(
    0,
    Math.floor((new Date(value).getTime() - Date.now()) / 1000),
  )
}
