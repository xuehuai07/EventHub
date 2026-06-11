import { useMutation, useQuery } from '@tanstack/react-query'
import { Button, Form, Input, Modal, Select, Space, message } from 'antd'
import { useState } from 'react'
import { createActivity, getCategories } from '../../entities/activity/api'
import type { ActivityRequest } from '../../shared/api/generated/types.gen'
import { ActivityCoverUploader } from './ActivityCoverUploader'

export function ActivityCreateModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean
  onClose: () => void
  onCreated: () => Promise<unknown>
}) {
  const [coverUploading, setCoverUploading] = useState(false)
  const categories = useQuery({
    queryKey: ['activity-categories'],
    queryFn: getCategories,
  })
  const mutation = useMutation({
    mutationFn: createActivity,
    onSuccess: async () => {
      message.success('活动草稿已创建')
      onClose()
      await onCreated()
    },
  })

  return (
    <Modal
      title="创建活动草稿"
      open={open}
      footer={null}
      width={620}
      onCancel={onClose}
    >
      <Form<ActivityRequest>
        layout="vertical"
        initialValues={{ version: 0 }}
        onFinish={(values) => {
          if (coverUploading) {
            message.warning('请等待封面上传完成')
            return
          }
          mutation.mutate(values)
        }}
      >
        <Form.Item name="title" label="活动名称" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Space.Compact block>
          <Form.Item
            name="categoryId"
            label="分类"
            rules={[{ required: true }]}
          >
            <Select
              style={{ width: 190 }}
              options={categories.data?.map((item) => ({
                label: item.name,
                value: item.id,
              }))}
            />
          </Form.Item>
          <Form.Item name="city" label="城市" rules={[{ required: true }]}>
            <Input />
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
          extra="封面会展示在用户端活动列表和详情页"
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
          {coverUploading ? '正在上传封面' : '保存草稿'}
        </Button>
      </Form>
    </Modal>
  )
}
