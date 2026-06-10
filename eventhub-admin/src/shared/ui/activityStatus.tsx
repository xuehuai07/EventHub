import { Tag } from 'antd'

const statusMeta = {
  DRAFT: ['草稿', 'default'],
  PENDING_REVIEW: ['待审核', 'processing'],
  PUBLISHED: ['已发布', 'success'],
  REJECTED: ['已驳回', 'error'],
  OFF_SHELF: ['已下架', 'warning'],
  FINISHED: ['已结束', 'default'],
} as const

export function ActivityStatusTag({ status }: { status?: string }) {
  const meta = statusMeta[status as keyof typeof statusMeta] ?? [
    status || '未知',
    'default',
  ]
  return <Tag color={meta[1]}>{meta[0]}</Tag>
}
