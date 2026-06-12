import { BrowserMultiFormatReader } from '@zxing/browser'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  CheckCircleOutlined,
  QrcodeOutlined,
  ScanOutlined,
  StopOutlined,
} from '@ant-design/icons'
import { Alert, Button, Input, Modal, Table, Tag, message } from 'antd'
import { useEffect, useRef, useState } from 'react'
import {
  getVerificationLogs,
  previewTicket,
  verifyTicket,
} from '../entities/ticket/api'
import type { VerificationResultView } from '../shared/api/generated/types.gen'
import { apiErrorMessage } from '../shared/api/apiErrorMessage'
import { useAuthStore } from '../shared/auth/authStore'
import '../features/verification/verification.css'

export function TicketVerificationPage() {
  const queryClient = useQueryClient()
  const isAdmin =
    useAuthStore((state) => state.user?.roles?.includes('ADMIN')) ?? false
  const [code, setCode] = useState('')
  const [candidate, setCandidate] = useState<VerificationResultView>()
  const [scanning, setScanning] = useState(false)
  const videoRef = useRef<HTMLVideoElement>(null)
  const controlsRef = useRef<{ stop: () => void } | undefined>(undefined)
  const logs = useQuery({
    queryKey: ['verification-logs', isAdmin],
    queryFn: () => getVerificationLogs(isAdmin),
  })
  const previewMutation = useMutation({
    mutationFn: (value: string) => previewTicket({ code: value }),
    onSuccess: setCandidate,
    onError: (error) => message.error(apiErrorMessage(error, '票码无法识别')),
  })
  const verifyMutation = useMutation({
    mutationFn: () => verifyTicket({ code, deviceId: browserDevice() }),
    onSuccess: async (result) => {
      setCandidate(result)
      message.success(result?.alreadyUsed ? '该票券此前已核销' : '核销成功')
      await queryClient.invalidateQueries({ queryKey: ['verification-logs'] })
    },
    onError: (error) => message.error(apiErrorMessage(error, '核销失败')),
  })

  useEffect(() => () => controlsRef.current?.stop(), [])

  const submitCode = (value: string) => {
    const normalized = value.trim()
    if (!normalized) return
    setCode(normalized)
    previewMutation.mutate(normalized)
  }

  const startScanner = async () => {
    setScanning(true)
    try {
      const reader = new BrowserMultiFormatReader()
      controlsRef.current = await reader.decodeFromVideoDevice(
        undefined,
        videoRef.current!,
        (result) => {
          if (!result) return
          controlsRef.current?.stop()
          setScanning(false)
          submitCode(result.getText())
        },
      )
    } catch {
      setScanning(false)
      message.error('无法打开摄像头，请检查浏览器权限或使用手工输入')
    }
  }

  return (
    <section className="verification-page">
      <div className="verification-heading">
        <div>
          <span>ENTRY CONTROL</span>
          <h1>{isAdmin ? '核销审计' : '票券核销'}</h1>
          <p>
            {isAdmin
              ? '查看平台核销记录，不代替商家执行核销。'
              : '扫描动态二维码或输入票券编码完成入场。'}
          </p>
        </div>
        <div className="verification-live">
          <i /> 现场服务在线
        </div>
      </div>

      {!isAdmin && (
        <div className="verification-console">
          <div className="scanner-panel">
            <div className="scanner-frame">
              {scanning ? (
                <video ref={videoRef} muted playsInline />
              ) : (
                <QrcodeOutlined />
              )}
              <span className="scan-corner corner-a" />
              <span className="scan-corner corner-b" />
            </div>
            <Button
              type={scanning ? 'default' : 'primary'}
              icon={scanning ? <StopOutlined /> : <ScanOutlined />}
              onClick={() => {
                if (scanning) {
                  controlsRef.current?.stop()
                  setScanning(false)
                } else void startScanner()
              }}
            >
              {scanning ? '停止扫描' : '打开摄像头'}
            </Button>
          </div>
          <div className="manual-panel">
            <span>手工核销</span>
            <h2>输入票券编码</h2>
            <p>摄像头不可用时，可输入以 ET 开头的永久票号。</p>
            <Input.Search
              size="large"
              value={code}
              placeholder="ET..."
              enterButton="查询票券"
              loading={previewMutation.isPending}
              onChange={(event) => setCode(event.target.value)}
              onSearch={submitCode}
            />
          </div>
        </div>
      )}

      <div className="verification-history">
        <h2>近期核销记录</h2>
        <Table
          rowKey="id"
          loading={logs.isPending}
          dataSource={logs.data?.items}
          columns={[
            { title: '时间', dataIndex: 'verifiedAt', render: formatTime },
            { title: '活动', dataIndex: 'activityTitle' },
            { title: '场次', dataIndex: 'sessionName' },
            { title: '操作人', dataIndex: 'operatorName' },
            {
              title: '结果',
              dataIndex: 'result',
              render: (value) => (
                <Tag color={value === 'SUCCESS' ? 'success' : 'warning'}>
                  {value === 'SUCCESS' ? '核销成功' : '重复扫描'}
                </Tag>
              ),
            },
          ]}
        />
      </div>

      <Modal
        title="核对票券信息"
        open={Boolean(candidate)}
        okText={candidate?.ticket?.status === 'UNUSED' ? '确认核销' : '关闭'}
        cancelButtonProps={{
          style: {
            display:
              candidate?.ticket?.status === 'UNUSED' ? undefined : 'none',
          },
        }}
        confirmLoading={verifyMutation.isPending}
        onCancel={() => setCandidate(undefined)}
        onOk={() =>
          candidate?.ticket?.status === 'UNUSED'
            ? verifyMutation.mutate()
            : setCandidate(undefined)
        }
      >
        {candidate?.ticket && (
          <div className="verification-result">
            {candidate.ticket.status === 'USED' && (
              <Alert type="warning" showIcon message="该票券已经核销" />
            )}
            <CheckCircleOutlined />
            <h2>{candidate.ticket.activityTitle}</h2>
            <p>
              {candidate.ticket.sessionName} · {candidate.ticket.venueName}
            </p>
            <dl>
              <dt>持票人</dt>
              <dd>{candidate.ticket.userDisplayName}</dd>
              <dt>票档</dt>
              <dd>{candidate.ticket.ticketTypeName}</dd>
              <dt>座位</dt>
              <dd>{seat(candidate.ticket)}</dd>
              <dt>票号</dt>
              <dd>{candidate.ticket.ticketNo}</dd>
            </dl>
          </div>
        )}
      </Modal>
    </section>
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
function browserDevice() {
  return `${navigator.platform || 'browser'}:${navigator.userAgent.slice(0, 80)}`
}
