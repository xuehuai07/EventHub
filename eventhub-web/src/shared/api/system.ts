import './http'
import { status } from './generated/sdk.gen'

export async function getSystemStatus() {
  const response = await status({ throwOnError: true })
  const payload = response.data

  if (!payload.data) {
    throw new Error('系统状态响应缺少数据')
  }

  return {
    ...payload,
    data: payload.data,
  }
}
