import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button,
  Descriptions,
  Drawer,
  Input,
  Modal,
  Space,
  Table,
  message,
} from 'antd'
import { useState } from 'react'
import {
  approveActivity,
  getPendingReviews,
  getReviewDetail,
  rejectActivity,
} from '../entities/activity/api'
import type {
  ActivityDetailView,
  ActivitySummaryView,
} from '../shared/api/generated/types.gen'
import { ActivityStatusTag } from '../shared/ui/activityStatus'
import '../features/activity/admin-business.css'

export function ActivityReviewPage() {
  const queryClient = useQueryClient()
  const [selectedId, setSelectedId] = useState<number>()
  const [rejectId, setRejectId] = useState<number>()
  const [reason, setReason] = useState('')
  const reviews = useQuery({
    queryKey: ['activity-reviews'],
    queryFn: getPendingReviews,
  })
  const detail = useQuery({
    queryKey: ['activity-review-detail', selectedId],
    queryFn: () => getReviewDetail(selectedId!),
    enabled: Boolean(selectedId),
  })
  const refresh = () =>
    queryClient.invalidateQueries({ queryKey: ['activity-reviews'] })
  const approveMutation = useMutation({
    mutationFn: approveActivity,
    onSuccess: async () => {
      message.success('活动已审核通过并发布')
      setSelectedId(undefined)
      await refresh()
    },
  })
  const rejectMutation = useMutation({
    mutationFn: ({ id, text }: { id: number; text: string }) =>
      rejectActivity(id, { reason: text }),
    onSuccess: async () => {
      message.success('活动已驳回')
      setRejectId(undefined)
      setReason('')
      await refresh()
    },
  })

  return (
    <section className="business-page">
      <div className="business-heading">
        <div>
          <span>平台治理</span>
          <h1>活动审核</h1>
          <p>核对活动内容、场馆、时间和票档后决定是否公开发布。</p>
        </div>
      </div>
      <Table
        rowKey="id"
        loading={reviews.isPending}
        dataSource={reviews.data?.items}
        columns={[
          {
            title: '活动',
            render: (_, row: ActivitySummaryView) => (
              <div className="table-primary">
                <strong>{row.title}</strong>
                <span>{row.merchantName}</span>
              </div>
            ),
          },
          { title: '分类', dataIndex: 'categoryName' },
          { title: '城市', dataIndex: 'city' },
          {
            title: '状态',
            render: (_, row: ActivitySummaryView) => (
              <ActivityStatusTag status={row.status} />
            ),
          },
          {
            title: '操作',
            render: (_, row: ActivitySummaryView) => (
              <Space>
                <Button type="link" onClick={() => setSelectedId(row.id)}>
                  查看详情
                </Button>
                <Button
                  type="link"
                  onClick={() => row.id && approveMutation.mutate(row.id)}
                >
                  通过
                </Button>
                <Button danger type="link" onClick={() => setRejectId(row.id)}>
                  驳回
                </Button>
              </Space>
            ),
          },
        ]}
      />
      <ReviewDrawer
        detail={detail.data}
        open={Boolean(selectedId)}
        onClose={() => setSelectedId(undefined)}
        onApprove={() => selectedId && approveMutation.mutate(selectedId)}
      />
      <Modal
        title="填写驳回原因"
        open={Boolean(rejectId)}
        confirmLoading={rejectMutation.isPending}
        onCancel={() => setRejectId(undefined)}
        onOk={() =>
          rejectId &&
          reason.trim() &&
          rejectMutation.mutate({ id: rejectId, text: reason })
        }
      >
        <Input.TextArea
          rows={4}
          value={reason}
          maxLength={500}
          onChange={(event) => setReason(event.target.value)}
        />
      </Modal>
    </section>
  )
}

function ReviewDrawer({
  detail,
  open,
  onClose,
  onApprove,
}: {
  detail?: ActivityDetailView
  open: boolean
  onClose: () => void
  onApprove: () => void
}) {
  return (
    <Drawer title={detail?.title} open={open} width={680} onClose={onClose}>
      {detail && (
        <>
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="商家">
              {detail.merchantName}
            </Descriptions.Item>
            <Descriptions.Item label="城市">{detail.city}</Descriptions.Item>
            <Descriptions.Item label="分类">
              {detail.categoryName}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <ActivityStatusTag status={detail.status} />
            </Descriptions.Item>
            <Descriptions.Item label="简介" span={2}>
              {detail.summary}
            </Descriptions.Item>
            <Descriptions.Item label="详情" span={2}>
              {detail.description}
            </Descriptions.Item>
          </Descriptions>
          <div className="review-sessions">
            {detail.sessions?.map((session) => (
              <article key={session.id}>
                <strong>{session.name}</strong>
                <span>
                  {session.venueName} · {session.startAt?.replace('T', ' ')}
                </span>
                <p>
                  {session.ticketTypes
                    ?.map(
                      (ticket) =>
                        `${ticket.name} ¥${((ticket.priceCents ?? 0) / 100).toFixed(2)}`,
                    )
                    .join(' / ')}
                </p>
              </article>
            ))}
          </div>
          <Button block type="primary" onClick={onApprove}>
            审核通过并发布
          </Button>
        </>
      )}
    </Drawer>
  )
}
