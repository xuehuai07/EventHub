import {
  detail2,
  detail3,
  list4,
  list5,
} from '../../shared/api/generated/sdk.gen'

export async function getManagedOrders(isAdmin: boolean) {
  const response = isAdmin
    ? await list5({
        query: { page: 1, pageSize: 100 },
        throwOnError: true,
      })
    : await list4({
        query: { page: 1, pageSize: 100 },
        throwOnError: true,
      })
  return response.data.data
}

export async function getManagedOrder(isAdmin: boolean, orderId: number) {
  const response = isAdmin
    ? await detail3({ path: { orderId }, throwOnError: true })
    : await detail2({ path: { orderId }, throwOnError: true })
  return response.data.data
}
