import type { MessageView } from '../../shared/api/generated/types.gen'

export type UiMessage = MessageView & {
  localId?: string
  pending?: boolean
  failed?: boolean
}

export const quickPrompts = [
  '推荐本周末适合两个人的活动',
  '查看我的待使用票券',
  '查询最近已支付订单',
]
