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
import { useEffect } from 'react'
import {
  addActivitySession,
  updateActivitySession,
} from '../../entities/activity/api'
import { getVenues } from '../../entities/venue/api'
import type {
  ActivitySummaryView,
  SessionView,
} from '../../shared/api/generated/types.gen'

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
  session,
  onClose,
  onCreated,
}: {
  activity?: ActivitySummaryView
  session?: SessionView
  onClose: () => void
  onCreated: () => Promise<unknown>
}) {
  const [form] = Form.useForm<SessionForm>()
  const venues = useQuery({ queryKey: ['merchant-venues'], queryFn: getVenues })
  const mutation = useMutation({
    mutationFn: (values: SessionForm) => {
      const body = {
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
      }
      return session?.id
        ? updateActivitySession(activity!.id!, session.id, body)
        : addActivitySession(activity!.id!, body)
    },
    onSuccess: async () => {
      message.success(session ? '场次和票档已更新' : '场次和票档已添加')
      onClose()
      await onCreated()
    },
    onError: (error) => {
      message.error(apiErrorMessage(error))
    },
  })

  useEffect(() => {
    if (!activity) {
      form.resetFields()
      return
    }
    const ticket = session?.ticketTypes?.[0]
    form.setFieldsValue(
      session
        ? {
            venueId: session.venueId,
            name: session.name,
            startAt: inputDateTime(session.startAt),
            endAt: inputDateTime(session.endAt),
            saleStartAt: inputDateTime(session.saleStartAt),
            saleEndAt: inputDateTime(session.saleEndAt),
            ticketName: ticket?.name,
            seatGrade: ticket?.seatGrade,
            priceYuan: (ticket?.priceCents ?? 0) / 100,
            totalStock: ticket?.totalStock,
            saleLimitPerUser: ticket?.saleLimitPerUser,
          }
        : {
            venueId: undefined,
            name: undefined,
            startAt: undefined,
            endAt: undefined,
            saleStartAt: undefined,
            saleEndAt: undefined,
            ticketName: '标准票',
            seatGrade: undefined,
            priceYuan: 99,
            totalStock: 100,
            saleLimitPerUser: 6,
          },
    )
  }, [activity, form, session])

  return (
    <Modal
      title={`${session ? '编辑' : '添加'}场次 · ${activity?.title ?? ''}`}
      open={Boolean(activity)}
      footer={null}
      width={680}
      onCancel={onClose}
    >
      <Form<SessionForm>
        form={form}
        layout="vertical"
        initialValues={{
          ticketName: '标准票',
          priceYuan: 99,
          totalStock: 100,
          saleLimitPerUser: 6,
        }}
        onFinish={(values) => mutation.mutate(values)}
        onFinishFailed={({ errorFields }) => {
          message.warning('请检查标红的场次信息')
          if (errorFields[0]) {
            form.scrollToField(errorFields[0].name, { block: 'center' })
          }
        }}
      >
        <Space.Compact block>
          <Form.Item
            name="venueId"
            label="场馆"
            rules={[{ required: true, message: '请选择场馆' }]}
          >
            <Select
              style={{ width: 230 }}
              options={venues.data?.map((item) => ({
                label: item.name,
                value: item.id,
              }))}
            />
          </Form.Item>
          <Form.Item
            name="name"
            label="场次名称"
            rules={[{ required: true, message: '请输入场次名称' }]}
          >
            <Input />
          </Form.Item>
        </Space.Compact>
        <div className="form-grid-two">
          <Form.Item
            name="startAt"
            label="开始时间"
            rules={[{ required: true, message: '请选择开始时间' }]}
          >
            <Input type="datetime-local" />
          </Form.Item>
          <Form.Item
            name="endAt"
            label="结束时间"
            dependencies={['startAt']}
            rules={[
              { required: true, message: '请选择结束时间' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  const startAt = getFieldValue('startAt')
                  return !value ||
                    !startAt ||
                    toTimestamp(value) > toTimestamp(startAt)
                    ? Promise.resolve()
                    : Promise.reject(new Error('结束时间必须晚于开始时间'))
                },
              }),
            ]}
          >
            <Input type="datetime-local" />
          </Form.Item>
          <Form.Item
            name="saleStartAt"
            label="开售时间"
            rules={[{ required: true, message: '请选择开售时间' }]}
          >
            <Input type="datetime-local" />
          </Form.Item>
          <Form.Item
            name="saleEndAt"
            label="停售时间"
            dependencies={['saleStartAt', 'startAt']}
            extra="停售时间不得晚于场次开始时间"
            rules={[
              { required: true, message: '请选择停售时间' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value) return Promise.resolve()
                  const saleStartAt = getFieldValue('saleStartAt')
                  const startAt = getFieldValue('startAt')
                  if (
                    saleStartAt &&
                    toTimestamp(value) <= toTimestamp(saleStartAt)
                  ) {
                    return Promise.reject(new Error('停售时间必须晚于开售时间'))
                  }
                  if (startAt && toTimestamp(value) > toTimestamp(startAt)) {
                    return Promise.reject(
                      new Error('停售时间不得晚于场次开始时间'),
                    )
                  }
                  return Promise.resolve()
                },
              }),
            ]}
          >
            <Input type="datetime-local" />
          </Form.Item>
        </div>
        <div className="ticket-form-row">
          <Form.Item
            name="ticketName"
            label="票档名称"
            rules={[{ required: true, message: '请输入票档名称' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="seatGrade" label="座位等级">
            <Input placeholder="固定座位可填写" />
          </Form.Item>
          <Form.Item
            name="priceYuan"
            label="价格（元）"
            rules={[{ required: true, message: '请输入价格' }]}
          >
            <InputNumber min={0} precision={2} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="totalStock"
            label="库存"
            rules={[{ required: true, message: '请输入库存' }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </div>
        <Form.Item
          name="saleLimitPerUser"
          label="每人限购"
          rules={[{ required: true, message: '请输入每人限购数量' }]}
        >
          <InputNumber min={1} max={20} style={{ width: 110 }} />
        </Form.Item>
        <Button
          block
          type="primary"
          htmlType="submit"
          loading={mutation.isPending}
        >
          {session ? '更新场次与票档' : '保存场次与票档'}
        </Button>
      </Form>
    </Modal>
  )
}

function toTimestamp(value: string) {
  return new Date(value).getTime()
}

function inputDateTime(value?: string) {
  return value?.slice(0, 16)
}

function apiErrorMessage(error: unknown) {
  const response = (
    error as {
      response?: { data?: { message?: string } }
    }
  ).response
  return response?.data?.message || '保存失败，请检查场次和票档信息后重试'
}
