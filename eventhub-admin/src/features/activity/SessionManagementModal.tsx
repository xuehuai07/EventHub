import {
  CalendarOutlined,
  DeleteOutlined,
  EditOutlined,
  EnvironmentOutlined,
  PlusOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Empty, Modal, Popconfirm, Tag, message } from 'antd'
import {
  getMerchantActivityDetail,
  removeActivitySession,
} from '../../entities/activity/api'
import { apiErrorMessage } from '../../shared/api/apiErrorMessage'
import type {
  ActivitySummaryView,
  SessionView,
} from '../../shared/api/generated/types.gen'

export function SessionManagementModal({
  activity,
  onClose,
  onAdd,
  onEdit,
  onChanged,
}: {
  activity?: ActivitySummaryView
  onClose: () => void
  onAdd: () => void
  onEdit: (session: SessionView) => void
  onChanged: () => Promise<unknown>
}) {
  const queryClient = useQueryClient()
  const detail = useQuery({
    queryKey: ['merchant-activity-detail', activity?.id],
    queryFn: () => getMerchantActivityDetail(activity!.id!),
    enabled: Boolean(activity?.id),
  })
  const removeMutation = useMutation({
    mutationFn: (sessionId: number) =>
      removeActivitySession(activity!.id!, sessionId),
    onSuccess: async () => {
      message.success('场次已删除')
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ['merchant-activity-detail', activity?.id],
        }),
        onChanged(),
      ])
    },
    onError: (error) => {
      message.error(apiErrorMessage(error, '删除场次失败，请稍后重试'))
    },
  })

  return (
    <Modal
      title={`管理场次 · ${activity?.title ?? ''}`}
      open={Boolean(activity)}
      width={760}
      footer={null}
      onCancel={onClose}
    >
      <div className="session-manager-intro">
        <div>
          <strong>场次与票档</strong>
          <span>固定座位场次的票档等级必须覆盖场馆全部座位等级。</span>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={onAdd}>
          添加场次
        </Button>
      </div>

      <div className="session-manager-list" aria-busy={detail.isPending}>
        {!detail.isPending && !detail.data?.sessions?.length ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="尚未配置场次"
          />
        ) : (
          detail.data?.sessions?.map((session) => (
            <SessionCard
              key={session.id}
              session={session}
              deleting={removeMutation.isPending}
              onEdit={() => onEdit(session)}
              onDelete={() => session.id && removeMutation.mutate(session.id)}
            />
          ))
        )}
      </div>
    </Modal>
  )
}

function SessionCard({
  session,
  deleting,
  onEdit,
  onDelete,
}: {
  session: SessionView
  deleting: boolean
  onEdit: () => void
  onDelete: () => void
}) {
  const requiredGrades = unique(
    session.seatAreas?.map((area) => area.seatGrade) ?? [],
  )
  const configuredGrades = unique(
    session.ticketTypes?.map((ticket) => ticket.seatGrade) ?? [],
  )
  const missingGrades =
    session.seatMode === 'FIXED'
      ? requiredGrades.filter((grade) => !configuredGrades.includes(grade))
      : []

  return (
    <article className="session-manager-card">
      <div className="session-manager-card-head">
        <div>
          <strong>{session.name}</strong>
          <span className="session-manager-meta">
            <EnvironmentOutlined />
            {session.venueName}
          </span>
          <span className="session-manager-meta">
            <CalendarOutlined />
            {formatDateTime(session.startAt)}
          </span>
        </div>
        <div className="session-manager-actions">
          <Button type="text" icon={<EditOutlined />} onClick={onEdit}>
            编辑
          </Button>
          <Popconfirm
            title="删除这个场次？"
            description="场次、票档和座位快照会一并删除，此操作不可撤销。"
            okText="确认删除"
            cancelText="保留"
            okButtonProps={{ danger: true, loading: deleting }}
            onConfirm={onDelete}
          >
            <Button danger type="text" icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </div>
      </div>

      <div className="session-ticket-grid">
        {session.ticketTypes?.map((ticket) => (
          <div key={ticket.id} className="session-ticket">
            <div>
              <strong>{ticket.name}</strong>
              <span>
                ¥{((ticket.priceCents ?? 0) / 100).toFixed(2)} · 限购{' '}
                {ticket.saleLimitPerUser}
              </span>
            </div>
            <Tag color={ticket.seatGrade ? 'green' : 'red'}>
              {ticket.seatGrade || '未填写座位等级'}
            </Tag>
          </div>
        ))}
      </div>

      {missingGrades.length > 0 && (
        <div className="session-grade-warning">
          缺少票档等级：{missingGrades.join('、')}
        </div>
      )}
      {session.seatMode === 'FIXED' && missingGrades.length === 0 && (
        <div className="session-grade-ok">座位等级已完整匹配</div>
      )}
    </article>
  )
}

function unique(values: Array<string | undefined>) {
  return [...new Set(values.filter((value): value is string => Boolean(value)))]
}

function formatDateTime(value?: string) {
  return value?.replace('T', ' ').slice(0, 16) ?? '时间未设置'
}
