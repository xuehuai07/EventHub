import {
  bindStaff,
  create4,
  list3,
  updateStatus,
} from '../../shared/api/generated/sdk.gen'
import type { MerchantCreateRequest } from '../../shared/api/generated/types.gen'

export async function getMerchants() {
  return (await list3({ throwOnError: true })).data.data ?? []
}

export async function createMerchant(body: MerchantCreateRequest) {
  return (await create4({ body, throwOnError: true })).data.data
}

export async function setMerchantStatus(
  merchantId: number,
  status: 'ACTIVE' | 'DISABLED',
) {
  await updateStatus({
    path: { merchantId },
    body: { status },
    throwOnError: true,
  })
}

export async function addMerchantStaff(merchantId: number, identifier: string) {
  await bindStaff({
    path: { merchantId },
    body: { identifier },
    throwOnError: true,
  })
}
