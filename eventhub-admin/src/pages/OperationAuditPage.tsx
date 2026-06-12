import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  message,
} from 'antd'
import { useState } from 'react'
import {
  getAdminReviews,
  getOperationLogs,
  hideReview,
  restoreReview,
} from '../entities/operations/api'
import type {
  ActivityReviewView,
  OperationLogView,
} from '../shared/api/generated/types.gen'

export function OperationAuditPage() {
  return (
    <div className="audit-page">
      <section className="admin-page-heading">
        <span className="section-label">治理与追溯</span>
        <h1>操作审计</h1>
        <p>查询关键管理操作，并处理不符合平台规则的用户评价。</p>
      </section>
      <Tabs
        size="large"
        items={[
          {
            key: 'logs',
            label: '操作记录',
            children: <OperationLogs />,
          },
          {
            key: 'reviews',
            label: '评价治理',
            children: <ReviewModeration />,
          },
        ]}
      />
    </div>
  )
}

function OperationLogs() {
  const [action, setAction] = useState<string>()
  const [resourceType, setResourceType] = useState<string>()
  const logs = useQuery({
    queryKey: ['operation-logs', action, resourceType],
    queryFn: () => getOperationLogs({ action, resourceType }),
  })

  return (
    <section className="audit-panel">
      <Space className="audit-filters" wrap>
        <Select
          allowClear
          placeholder="操作类型"
          style={{ width: 190 }}
          value={action}
          onChange={setAction}
          options={[
            { value: 'ACTIVITY_APPROVE', label: '活动审核通过' },
            { value: 'ACTIVITY_REJECT', label: '活动驳回' },
            { value: 'ACTIVITY_OFF_SHELF', label: '活动下架' },
            { value: 'MERCHANT_STATUS_UPDATE', label: '商家状态修改' },
            { value: 'ACTIVITY_REVIEW_HIDE', label: '评价隐藏' },
            { value: 'ACTIVITY_REVIEW_RESTORE', label: '评价恢复' },
          ]}
        />
        <Select
          allowClear
          placeholder="资源类型"
          style={{ width: 160 }}
          value={resourceType}
          onChange={setResourceType}
          options={[
            'ACTIVITY',
            'SESSION',
            'VENUE',
            'MERCHANT',
            'ACTIVITY_REVIEW',
          ].map((value) => ({ value, label: value }))}
        />
      </Space>
      <Table<OperationLogView>
        rowKey="id"
        loading={logs.isPending}
        dataSource={logs.data?.items}
        pagination={false}
        columns={[
          {
            title: '时间',
            dataIndex: 'createdAt',
            width: 180,
            render: (value?: string) => formatDateTime(value),
          },
          {
            title: '操作人',
            render: (_, row) => (
              <span>
                <strong>{row.operatorName}</strong>
                <br />
                <small>{row.operatorRole}</small>
              </span>
            ),
          },
          {
            title: '动作',
            dataIndex: 'action',
            render: (value: string) => <Tag>{value}</Tag>,
          },
          {
            title: '资源',
            render: (_, row) => `${row.resourceType} #${row.resourceId}`,
          },
          { title: '摘要', dataIndex: 'summary' },
          {
            title: '请求编号',
            dataIndex: 'requestId',
            ellipsis: true,
          },
        ]}
      />
    </section>
  )
}

function ReviewModeration() {
  const [status, setStatus] = useState<string>()
  const [keyword, setKeyword] = useState('')
  const [selected, setSelected] = useState<ActivityReviewView>()
  const [reason, setReason] = useState('')
  const queryClient = useQueryClient()
  const [messageApi, contextHolder] = message.useMessage()
  const reviews = useQuery({
    queryKey: ['admin-activity-reviews', status, keyword],
    queryFn: () =>
      getAdminReviews({
        status,
        keyword: keyword || undefined,
      }),
  })
  const refresh = () =>
    queryClient.invalidateQueries({ queryKey: ['admin-activity-reviews'] })
  const hideMutation = useMutation({
    mutationFn: () => hideReview(selected?.id ?? 0, reason),
    onSuccess: async () => {
      setSelected(undefined)
      setReason('')
      await refresh()
      messageApi.success('评价已隐藏')
    },
  })
  const restoreMutation = useMutation({
    mutationFn: (reviewId: number) => restoreReview(reviewId),
    onSuccess: async () => {
      await refresh()
      messageApi.success('评价已恢复')
    },
  })

  return (
    <section className="audit-panel">
      {contextHolder}
      <Space className="audit-filters" wrap>
        <Input.Search
          allowClear
          placeholder="活动、用户或评价内容"
          style={{ width: 280 }}
          onSearch={setKeyword}
        />
        <Select
          allowClear
          placeholder="评价状态"
          style={{ width: 160 }}
          value={status}
          onChange={setStatus}
          options={[
            { value: 'PUBLISHED', label: '公开' },
            { value: 'HIDDEN', label: '已隐藏' },
          ]}
        />
      </Space>
      <Table<ActivityReviewView>
        rowKey="id"
        loading={reviews.isPending}
        dataSource={reviews.data?.items}
        pagination={false}
        columns={[
          {
            title: '活动与用户',
            render: (_, row) => (
              <span>
                <strong>{row.activityTitle}</strong>
                <br />
                <small>{row.userDisplayName}</small>
              </span>
            ),
          },
          {
            title: '评分',
            dataIndex: 'rating',
            width: 110,
            render: (value: number) => (
              <span className="audit-stars">{'★'.repeat(value)}</span>
            ),
          },
          { title: '评价内容', dataIndex: 'content' },
          {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (value: string) => (
              <Tag color={value === 'PUBLISHED' ? 'green' : 'red'}>
                {value === 'PUBLISHED' ? '公开' : '已隐藏'}
              </Tag>
            ),
          },
          {
            title: '操作',
            width: 100,
            render: (_, row) =>
              row.status === 'PUBLISHED' ? (
                <Button danger type="link" onClick={() => setSelected(row)}>
                  隐藏
                </Button>
              ) : (
                <Button
                  type="link"
                  loading={restoreMutation.isPending}
                  onClick={() => restoreMutation.mutate(row.id ?? 0)}
                >
                  恢复
                </Button>
              ),
          },
        ]}
      />
      <Modal
        title="隐藏评价"
        open={Boolean(selected)}
        okText="确认隐藏"
        okButtonProps={{
          danger: true,
          disabled: !reason.trim(),
          loading: hideMutation.isPending,
        }}
        onCancel={() => setSelected(undefined)}
        onOk={() => hideMutation.mutate()}
      >
        <p>评价隐藏后不会出现在用户端，也不再参与活动评分。</p>
        <Input.TextArea
          rows={4}
          maxLength={500}
          value={reason}
          placeholder="填写治理原因"
          onChange={(event) => setReason(event.target.value)}
        />
      </Modal>
    </section>
  )
}

function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '—'
}
