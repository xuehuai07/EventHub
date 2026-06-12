import { useQuery } from '@tanstack/react-query'
import QRCode from 'qrcode'
import { useEffect, useState } from 'react'
import { getTicketCredential } from '../../entities/ticket/api'

export function TicketQr({ ticketId }: { ticketId: number }) {
  const [image, setImage] = useState<string>()
  const credential = useQuery({
    queryKey: ['ticket-credential', ticketId],
    queryFn: () => getTicketCredential(ticketId),
    refetchInterval: 45_000,
  })

  useEffect(() => {
    if (!credential.data?.credential) return
    void QRCode.toDataURL(credential.data.credential, {
      width: 320,
      margin: 2,
      color: { dark: '#18231f', light: '#fffdf7' },
      errorCorrectionLevel: 'M',
    }).then(setImage)
  }, [credential.data?.credential])

  return (
    <div className="ticket-qr">
      {image ? (
        <img src={image} alt="动态入场二维码" />
      ) : (
        <span>正在生成动态二维码...</span>
      )}
      <small>二维码约每 60 秒更新，请在入场时出示</small>
    </div>
  )
}
