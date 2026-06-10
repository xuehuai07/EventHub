import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Form, Input, Modal, Space, Table, Tag, message } from 'antd'
import { useState } from 'react'
import {
  addMerchantStaff,
  createMerchant,
  getMerchants,
  setMerchantStatus,
} from '../entities/merchant/api'
import type {
  MerchantCreateRequest,
  MerchantView,
} from '../shared/api/generated/types.gen'
import '../features/activity/admin-business.css'

export function MerchantManagementPage() {
  const queryClient = useQueryClient()
  const [createOpen, setCreateOpen] = useState(false)
  const [staffMerchant, setStaffMerchant] = useState<MerchantView>()
  const merchants = useQuery({
    queryKey: ['admin-merchants'],
    queryFn: getMerchants,
  })
  const refresh = () =>
    queryClient.invalidateQueries({ queryKey: ['admin-merchants'] })
  const createMutation = useMutation({
    mutationFn: createMerchant,
    onSuccess: async () => {
      message.success('商家已创建')
      setCreateOpen(false)
      await refresh()
    },
  })
  const statusMutation = useMutation({
    mutationFn: ({
      id,
      status,
    }: {
      id: number
      status: 'ACTIVE' | 'DISABLED'
    }) => setMerchantStatus(id, status),
    onSuccess: async () => {
      message.success('商家状态已更新')
      await refresh()
    },
  })
  const staffMutation = useMutation({
    mutationFn: ({ id, identifier }: { id: number; identifier: string }) =>
      addMerchantStaff(id, identifier),
    onSuccess: async () => {
      message.success('用户已绑定为商家员工')
      setStaffMerchant(undefined)
      await refresh()
    },
  })

  return (
    <section className="business-page">
      <div className="business-heading">
        <div>
          <span>合作方管理</span>
          <h1>商家管理</h1>
          <p>创建平台商家，将已注册用户绑定为员工，并控制商家可用状态。</p>
        </div>
        <Button type="primary" onClick={() => setCreateOpen(true)}>
          创建商家
        </Button>
      </div>
      <Table
        rowKey="id"
        loading={merchants.isPending}
        dataSource={merchants.data}
        columns={[
          {
            title: '商家',
            render: (_, row: MerchantView) => (
              <div className="table-primary">
                <strong>{row.name}</strong>
                <span>{row.description || '暂无商家介绍'}</span>
              </div>
            ),
          },
          { title: '员工数', dataIndex: 'staffCount' },
          {
            title: '状态',
            render: (_, row: MerchantView) => (
              <Tag color={row.status === 'ACTIVE' ? 'success' : 'default'}>
                {row.status === 'ACTIVE' ? '正常' : '已停用'}
              </Tag>
            ),
          },
          {
            title: '操作',
            render: (_, row: MerchantView) => (
              <Space>
                <Button type="link" onClick={() => setStaffMerchant(row)}>
                  绑定员工
                </Button>
                <Button
                  type="link"
                  danger={row.status === 'ACTIVE'}
                  onClick={() =>
                    row.id &&
                    statusMutation.mutate({
                      id: row.id,
                      status: row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE',
                    })
                  }
                >
                  {row.status === 'ACTIVE' ? '停用' : '启用'}
                </Button>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title="创建商家"
        open={createOpen}
        footer={null}
        onCancel={() => setCreateOpen(false)}
      >
        <Form<MerchantCreateRequest>
          layout="vertical"
          onFinish={(values) => createMutation.mutate(values)}
        >
          <Form.Item name="name" label="商家名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="商家介绍">
            <Input.TextArea rows={4} />
          </Form.Item>
          <Button block type="primary" htmlType="submit">
            创建商家
          </Button>
        </Form>
      </Modal>
      <Modal
        title={`绑定员工 · ${staffMerchant?.name ?? ''}`}
        open={Boolean(staffMerchant)}
        footer={null}
        onCancel={() => setStaffMerchant(undefined)}
      >
        <Form
          layout="vertical"
          onFinish={({ identifier }) =>
            staffMerchant?.id &&
            staffMutation.mutate({ id: staffMerchant.id, identifier })
          }
        >
          <Form.Item
            name="identifier"
            label="已注册用户的用户名或手机号"
            rules={[{ required: true }]}
          >
            <Input />
          </Form.Item>
          <Button block type="primary" htmlType="submit">
            绑定员工
          </Button>
        </Form>
      </Modal>
    </section>
  )
}
