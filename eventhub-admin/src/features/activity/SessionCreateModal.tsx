import { useMutation, useQuery } from '@tanstack/react-query'
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  message,
} from 'antd'
import { addActivitySession } from '../../entities/activity/api'
import { getVenues } from '../../entities/venue/api'
import type { ActivitySummaryView } from '../../shared/api/generated/types.gen'

type SessionForm = {
  venueId: number
  name: string
  startAt: string
  endAt: string
  saleStartAt: string
  saleEndAt: string
  ticketName: string
  seatGrade?: string
  priceYuan: number
  totalStock: number
  saleLimitPerUser: number
}

export function SessionCreateModal({
  activity,
  onClose,
  onCreated,
}: {
  activity?: ActivitySummaryView
  onClose: () => void
  onCreated: () => Promise<unknown>
}) {
  const venues = useQuery({ queryKey: ['merchant-venues'], queryFn: getVenues })
  const mutation = useMutation({
    mutationFn: (values: SessionForm) =>
      addActivitySession(activity!.id!, {
        venueId: values.venueId,
        name: values.name,
        startAt: values.startAt,
        endAt: values.endAt,
        saleStartAt: values.saleStartAt,
        saleEndAt: values.saleEndAt,
        version: 0,
        ticketTypes: [
          {
            name: values.ticketName,
            seatGrade: values.seatGrade,
            priceCents: Math.round(values.priceYuan * 100),
            totalStock: values.totalStock,
            saleLimitPerUser: values.saleLimitPerUser,
          },
        ],
      }),
    onSuccess: async () => {
      message.success('场次和票档已添加')
      onClose()
      await onCreated()
    },
  })

  return (
    <Modal
      title={`添加场次 · ${activity?.title ?? ''}`}
      open={Boolean(activity)}
      footer={null}
      width={680}
      onCancel={onClose}
    >
      <Form<SessionForm>
        layout="vertical"
        initialValues={{
          ticketName: '标准票',
          priceYuan: 99,
          totalStock: 100,
          saleLimitPerUser: 6,
        }}
        onFinish={(values) => mutation.mutate(values)}
      >
        <Space.Compact block>
          <Form.Item name="venueId" label="场馆" rules={[{ required: true }]}>
            <Select
              style={{ width: 230 }}
              options={venues.data?.map((item) => ({
                label: item.name,
                value: item.id,
              }))}
            />
          </Form.Item>
          <Form.Item name="name" label="场次名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
        </Space.Compact>
        <div className="form-grid-two">
          {[
            ['startAt', '开始时间'],
            ['endAt', '结束时间'],
            ['saleStartAt', '开售时间'],
            ['saleEndAt', '停售时间'],
          ].map(([name, label]) => (
            <Form.Item
              key={name}
              name={name}
              label={label}
              rules={[{ required: true }]}
            >
              <Input type="datetime-local" />
            </Form.Item>
          ))}
        </div>
        <div className="ticket-form-row">
          <Form.Item name="ticketName" label="票档名称">
            <Input />
          </Form.Item>
          <Form.Item name="seatGrade" label="座位等级">
            <Input placeholder="固定座位可填写" />
          </Form.Item>
          <Form.Item name="priceYuan" label="价格（元）">
            <InputNumber min={0} precision={2} />
          </Form.Item>
          <Form.Item name="totalStock" label="库存">
            <InputNumber min={1} />
          </Form.Item>
        </div>
        <Form.Item name="saleLimitPerUser" label="每人限购">
          <InputNumber min={1} max={20} />
        </Form.Item>
        <Button
          block
          type="primary"
          htmlType="submit"
          loading={mutation.isPending}
        >
          保存场次与票档
        </Button>
      </Form>
    </Modal>
  )
}
