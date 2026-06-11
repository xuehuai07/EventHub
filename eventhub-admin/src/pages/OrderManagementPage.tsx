import { useQuery } from '@tanstack/react-query'
import { Button, Descriptions, Drawer, Table, Tag } from 'antd'
import { useState } from 'react'
import { getManagedOrder, getManagedOrders } from '../entities/order/api'
import { useAuthStore } from '../shared/auth/authStore'
import type { OrderView } from '../shared/api/generated/types.gen'
import '../features/order/order-management.css'

const statusMeta = {
  PENDING_PAYMENT: { color: 'gold', text: '待支付' },
  PAID: { color: 'green', text: '已支付' },
  CANCELLED: { color: 'default', text: '已取消' },
  EXPIRED: { color: 'red', text: '已过期' },
} as const

export function OrderManagementPage() {
  const isAdmin = useAuthStore(
    (state) => state.user?.roles?.includes('ADMIN') ?? false,
  )
  const [selectedId, setSelectedId] = useState<number>()
  const orders = useQuery({
    queryKey: ['managed-orders', isAdmin],
    queryFn: () => getManagedOrders(isAdmin),
  })
  const detail = useQuery({
    queryKey: ['managed-order', isAdmin, selectedId],
    queryFn: () => getManagedOrder(isAdmin, selectedId ?? 0),
    enabled: selectedId !== undefined,
  })

  return (
    <section className="business-page order-management">
      <div className="business-heading">
        <div>
          <span>{isAdmin ? '平台交易视图' : '商家交易视图'}</span>
          <h1>订单管理</h1>
          <p>
            只读查看订单状态、票档与座位明细，状态由用户操作和系统任务驱动。
          </p>
        </div>
      </div>
      <Table
        rowKey="id"
        loading={orders.isPending}
        dataSource={orders.data?.items}
        columns={[
          {
            title: '订单',
            render: (_, row: OrderView) => (
              <div className="table-primary">
                <strong>{row.activityTitle}</strong>
                <span>{row.orderNo}</span>
              </div>
            ),
          },
          { title: '场次', dataIndex: 'sessionName' },
          {
            title: '数量',
            dataIndex: 'totalQuantity',
            render: (value) => `${value} 张`,
          },
          {
            title: '金额',
            dataIndex: 'totalAmountCents',
            render: (value) => `¥${((value ?? 0) / 100).toFixed(2)}`,
          },
          {
            title: '状态',
            dataIndex: 'status',
            render: (status: keyof typeof statusMeta) => {
              const meta = statusMeta[status]
              return <Tag color={meta?.color}>{meta?.text ?? status}</Tag>
            },
          },
          {
            title: '操作',
            render: (_, row: OrderView) => (
              <Button type="link" onClick={() => setSelectedId(row.id)}>
                查看详情
              </Button>
            ),
          },
        ]}
      />
      <Drawer
        open={selectedId !== undefined}
        width={560}
        title="订单详情"
        onClose={() => setSelectedId(undefined)}
      >
        {detail.data && <OrderDescriptions order={detail.data} />}
      </Drawer>
    </section>
  )
}

function OrderDescriptions({ order }: { order: OrderView }) {
  return (
    <>
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="订单号">{order.orderNo}</Descriptions.Item>
        <Descriptions.Item label="活动">
          {order.activityTitle}
        </Descriptions.Item>
        <Descriptions.Item label="场次">{order.sessionName}</Descriptions.Item>
        <Descriptions.Item label="场馆">{order.venueName}</Descriptions.Item>
        <Descriptions.Item label="金额">
          ¥{((order.totalAmountCents ?? 0) / 100).toFixed(2)}
        </Descriptions.Item>
      </Descriptions>
      <div className="managed-order-items">
        <h3>票品明细</h3>
        {order.items?.map((item) => (
          <div key={item.id}>
            <span>
              <strong>{item.ticketTypeName}</strong>
              <small>
                {item.areaName
                  ? `${item.areaName} ${item.rowLabel}排 ${item.seatNumber}座`
                  : `${item.quantity} 张`}
              </small>
            </span>
            <b>¥{((item.subtotalCents ?? 0) / 100).toFixed(2)}</b>
          </div>
        ))}
      </div>
    </>
  )
}
