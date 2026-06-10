import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Space,
  Table,
  Tag,
  message,
} from 'antd'
import { useState } from 'react'
import {
  configureVenueSeats,
  createVenue,
  getVenues,
} from '../entities/venue/api'
import type {
  SeatGenerationRequest,
  VenueRequest,
  VenueView,
} from '../shared/api/generated/types.gen'
import '../features/activity/admin-business.css'

export function VenueManagementPage() {
  const queryClient = useQueryClient()
  const [createOpen, setCreateOpen] = useState(false)
  const [seatVenue, setSeatVenue] = useState<VenueView>()
  const venues = useQuery({ queryKey: ['merchant-venues'], queryFn: getVenues })
  const createMutation = useMutation({
    mutationFn: createVenue,
    onSuccess: async () => {
      message.success('场馆已创建')
      setCreateOpen(false)
      await queryClient.invalidateQueries({ queryKey: ['merchant-venues'] })
    },
  })
  const seatMutation = useMutation({
    mutationFn: ({
      venueId,
      body,
    }: {
      venueId: number
      body: SeatGenerationRequest
    }) => configureVenueSeats(venueId, body),
    onSuccess: async () => {
      message.success('固定座位已生成')
      setSeatVenue(undefined)
      await queryClient.invalidateQueries({ queryKey: ['merchant-venues'] })
    },
  })

  return (
    <section className="business-page">
      <div className="business-heading">
        <div>
          <span>空间资源</span>
          <h1>场馆与座位</h1>
          <p>先创建活动发生的空间，再为固定座位场馆生成区域和排座。</p>
        </div>
        <Button type="primary" onClick={() => setCreateOpen(true)}>
          新建场馆
        </Button>
      </div>
      <Table
        rowKey="id"
        loading={venues.isPending}
        dataSource={venues.data}
        columns={[
          { title: '场馆', dataIndex: 'name' },
          { title: '城市', dataIndex: 'city' },
          { title: '地址', dataIndex: 'address' },
          {
            title: '模式',
            render: (_, row: VenueView) => (
              <Tag color={row.seatMode === 'FIXED' ? 'gold' : 'blue'}>
                {row.seatMode === 'FIXED' ? '固定座位' : '自由入场'}
              </Tag>
            ),
          },
          { title: '容量', dataIndex: 'capacity' },
          {
            title: '操作',
            render: (_, row: VenueView) =>
              row.seatMode === 'FIXED' ? (
                <Button type="link" onClick={() => setSeatVenue(row)}>
                  生成座位
                </Button>
              ) : null,
          },
        ]}
      />
      <Modal
        title="新建场馆"
        open={createOpen}
        footer={null}
        onCancel={() => setCreateOpen(false)}
      >
        <Form<VenueRequest>
          layout="vertical"
          initialValues={{ seatMode: 'GENERAL', capacity: 100, version: 0 }}
          onFinish={(values) => createMutation.mutate(values)}
        >
          <Form.Item name="name" label="场馆名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Space.Compact block>
            <Form.Item name="city" label="城市" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item
              name="capacity"
              label="容量"
              rules={[{ required: true }]}
            >
              <InputNumber min={0} />
            </Form.Item>
          </Space.Compact>
          <Form.Item
            name="address"
            label="详细地址"
            rules={[{ required: true }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="seatMode" label="座位模式">
            <Radio.Group
              options={[
                { label: '自由入场', value: 'GENERAL' },
                { label: '固定座位', value: 'FIXED' },
              ]}
            />
          </Form.Item>
          <Button
            block
            type="primary"
            htmlType="submit"
            loading={createMutation.isPending}
          >
            创建场馆
          </Button>
        </Form>
      </Modal>
      <Modal
        title={`生成座位 · ${seatVenue?.name ?? ''}`}
        open={Boolean(seatVenue)}
        footer={null}
        onCancel={() => setSeatVenue(undefined)}
      >
        <Card size="small" className="form-note">
          本次生成会替换该场馆现有座位。单个场馆最多 5000 个座位。
        </Card>
        <Form
          layout="vertical"
          initialValues={{
            areaName: '主厅',
            seatGrade: 'STANDARD',
            rowPrefix: 'A',
            rowCount: 10,
            seatsPerRow: 12,
          }}
          onFinish={(values) =>
            seatVenue?.id &&
            seatMutation.mutate({
              venueId: seatVenue.id,
              body: { blocks: [values] },
            })
          }
        >
          <Space.Compact block>
            <Form.Item
              name="areaName"
              label="区域"
              rules={[{ required: true }]}
            >
              <Input />
            </Form.Item>
            <Form.Item
              name="seatGrade"
              label="座位等级"
              rules={[{ required: true }]}
            >
              <Input />
            </Form.Item>
          </Space.Compact>
          <Space.Compact block>
            <Form.Item name="rowPrefix" label="排号前缀">
              <Input />
            </Form.Item>
            <Form.Item name="rowCount" label="排数">
              <InputNumber min={1} max={100} />
            </Form.Item>
            <Form.Item name="seatsPerRow" label="每排座位">
              <InputNumber min={1} max={100} />
            </Form.Item>
          </Space.Compact>
          <Button
            block
            type="primary"
            htmlType="submit"
            loading={seatMutation.isPending}
          >
            生成座位
          </Button>
        </Form>
      </Modal>
    </section>
  )
}
