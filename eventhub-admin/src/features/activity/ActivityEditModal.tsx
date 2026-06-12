import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Spin,
  message,
} from 'antd'
import { useEffect, useState } from 'react'
import {
  getCategories,
  getMerchantActivityDetail,
  updateActivity,
} from '../../entities/activity/api'
import type {
  ActivityRequest,
  ActivitySummaryView,
} from '../../shared/api/generated/types.gen'
import { apiErrorMessage } from '../../shared/api/apiErrorMessage'
import { ActivityCoverUploader } from './ActivityCoverUploader'

export function ActivityEditModal({
  activity,
  onClose,
  onUpdated,
}: {
  activity?: ActivitySummaryView
  onClose: () => void
  onUpdated: () => Promise<unknown>
}) {
  const [form] = Form.useForm<ActivityRequest>()
  const queryClient = useQueryClient()
  const [coverUploading, setCoverUploading] = useState(false)
  const activityId = activity?.id
  const detail = useQuery({
    queryKey: ['merchant-activity-detail', activityId],
    queryFn: () => getMerchantActivityDetail(activityId!),
    enabled: Boolean(activityId),
  })
  const categories = useQuery({
    queryKey: ['activity-categories'],
    queryFn: getCategories,
    enabled: Boolean(activityId),
  })
  const mutation = useMutation({
    mutationFn: (body: ActivityRequest) => updateActivity(activityId!, body),
    onSuccess: async () => {
      message.success('活动信息已更新')
      onClose()
      await queryClient.invalidateQueries({
        queryKey: ['merchant-activity-detail', activityId],
      })
      await onUpdated()
    },
    onError: (error) => {
      message.error(apiErrorMessage(error, '更新失败，请刷新后重试'))
    },
  })
  const published = detail.data?.status === 'PUBLISHED'

  useEffect(() => {
    if (!detail.data) return
    form.setFieldsValue({
      categoryId: detail.data.categoryId,
      title: detail.data.title,
      summary: detail.data.summary,
      description: detail.data.description,
      coverUrl: detail.data.coverUrl,
      city: detail.data.city,
      version: detail.data.version,
    })
  }, [detail.data, form])

  const close = () => {
    form.resetFields()
    onClose()
  }

  return (
    <Modal
      title={`编辑活动信息${activity?.title ? ` · ${activity.title}` : ''}`}
      open={Boolean(activity)}
      footer={null}
      width={620}
      destroyOnHidden
      onCancel={close}
    >
      {detail.isPending ? (
        <div className="activity-form-loading">
          <Spin />
        </div>
      ) : detail.isError || !detail.data ? (
        <Alert
          type="error"
          showIcon
          message="活动详情加载失败"
          description="请关闭窗口后重试。"
        />
      ) : (
        <Form<ActivityRequest>
          form={form}
          layout="vertical"
          onFinish={(values) => {
            if (coverUploading) {
              message.warning('请等待封面上传完成')
              return
            }
            mutation.mutate({
              ...values,
              categoryId: published
                ? detail.data!.categoryId!
                : values.categoryId,
              title: published ? detail.data!.title! : values.title,
              city: published ? detail.data!.city! : values.city,
              version: detail.data!.version,
            })
          }}
        >
          {published && (
            <Alert
              className="form-note"
              type="info"
              showIcon
              message="已发布活动仅可更新展示内容"
              description="标题、分类、城市、场次和票档保持不变，保存后用户端立即展示新封面和描述。"
            />
          )}
          <Form.Item name="title" label="活动名称" rules={[{ required: true }]}>
            <Input disabled={published} />
          </Form.Item>
          <Space.Compact block>
            <Form.Item
              name="categoryId"
              label="分类"
              rules={[{ required: true }]}
            >
              <Select
                disabled={published}
                style={{ width: 190 }}
                options={categories.data?.map((item) => ({
                  label: item.name,
                  value: item.id,
                }))}
              />
            </Form.Item>
            <Form.Item name="city" label="城市" rules={[{ required: true }]}>
              <Input disabled={published} />
            </Form.Item>
          </Space.Compact>
          <Form.Item
            name="summary"
            label="一句话介绍"
            rules={[{ required: true }]}
          >
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item
            name="description"
            label="活动详情"
            rules={[{ required: true }]}
          >
            <Input.TextArea rows={5} />
          </Form.Item>
          <Form.Item
            name="coverUrl"
            label="活动封面"
            extra="替换后会更新用户端活动列表和详情页"
          >
            <ActivityCoverUploader
              disabled={mutation.isPending}
              onUploadingChange={setCoverUploading}
            />
          </Form.Item>
          <Button
            block
            type="primary"
            htmlType="submit"
            disabled={coverUploading}
            loading={mutation.isPending || coverUploading}
          >
            {coverUploading ? '正在上传封面' : '保存修改'}
          </Button>
        </Form>
      )}
    </Modal>
  )
}
