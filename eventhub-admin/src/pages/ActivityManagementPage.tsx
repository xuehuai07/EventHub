import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Space, Table, message } from 'antd'
import { useState } from 'react'
import { getMerchantActivities, submitActivity } from '../entities/activity/api'
import { ActivityCreateModal } from '../features/activity/ActivityCreateModal'
import { SessionCreateModal } from '../features/activity/SessionCreateModal'
import { apiErrorMessage } from '../shared/api/apiErrorMessage'
import type { ActivitySummaryView } from '../shared/api/generated/types.gen'
import { ActivityStatusTag } from '../shared/ui/activityStatus'
import '../features/activity/admin-business.css'

export function ActivityManagementPage() {
  const queryClient = useQueryClient()
  const [createOpen, setCreateOpen] = useState(false)
  const [sessionActivity, setSessionActivity] = useState<ActivitySummaryView>()
  const activities = useQuery({
    queryKey: ['merchant-activities'],
    queryFn: getMerchantActivities,
  })
  const refresh = () =>
    queryClient.invalidateQueries({ queryKey: ['merchant-activities'] })
  const submitMutation = useMutation({
    mutationFn: submitActivity,
    onSuccess: async () => {
      message.success('活动已提交平台审核')
      await refresh()
    },
    onError: (error) => {
      message.error(
        apiErrorMessage(error, '提交审核失败，请检查场次和票档后重试'),
      )
    },
  })

  return (
    <section className="business-page">
      <div className="business-heading">
        <div>
          <span>活动工作流</span>
          <h1>活动管理</h1>
          <p>从草稿开始配置场次和票档，信息完整后提交平台审核。</p>
        </div>
        <Button type="primary" onClick={() => setCreateOpen(true)}>
          创建活动
        </Button>
      </div>
      <Table
        rowKey="id"
        loading={activities.isPending}
        dataSource={activities.data?.items}
        columns={[
          {
            title: '活动',
            dataIndex: 'title',
            render: (title, row: ActivitySummaryView) => (
              <div className="table-primary">
                <strong>{title}</strong>
                <span>{row.summary}</span>
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
                {['DRAFT', 'REJECTED'].includes(row.status ?? '') && (
                  <>
                    <Button type="link" onClick={() => setSessionActivity(row)}>
                      添加场次
                    </Button>
                    <Button
                      type="link"
                      loading={submitMutation.isPending}
                      onClick={() => row.id && submitMutation.mutate(row.id)}
                    >
                      提交审核
                    </Button>
                  </>
                )}
              </Space>
            ),
          },
        ]}
      />
      <ActivityCreateModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={refresh}
      />
      <SessionCreateModal
        activity={sessionActivity}
        onClose={() => setSessionActivity(undefined)}
        onCreated={refresh}
      />
    </section>
  )
}
