import {
  cancel,
  create,
  create1,
  detail2,
  get,
  get1,
  list,
  pay,
  release,
} from '../../shared/api/generated/sdk.gen'
import type {
  CreateOrderRequest,
  SeatLockRequest,
} from '../../shared/api/generated/types.gen'

export async function getAvailability(sessionId: number) {
  return (await get({ path: { sessionId }, throwOnError: true })).data.data
}

export async function createSeatLock(body: SeatLockRequest) {
  return (await create({ body, throwOnError: true })).data.data
}

export async function getSeatLock(lockNo: string) {
  return (await get1({ path: { lockNo }, throwOnError: true })).data.data
}

export async function releaseSeatLock(lockNo: string) {
  return (await release({ path: { lockNo }, throwOnError: true })).data.data
}

export async function createOrder(body: CreateOrderRequest) {
  return (
    await create1({
      body,
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      throwOnError: true,
    })
  ).data.data
}

export async function getOrders() {
  return (await list({ query: { page: 1, pageSize: 50 }, throwOnError: true }))
    .data.data
}

export async function getOrder(orderId: number) {
  return (await detail2({ path: { orderId }, throwOnError: true })).data.data
}

export async function payOrder(orderId: number) {
  return (
    await pay({
      path: { orderId },
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      throwOnError: true,
    })
  ).data.data
}

export async function cancelOrder(orderId: number) {
  return (
    await cancel({
      path: { orderId },
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      throwOnError: true,
    })
  ).data.data
}
