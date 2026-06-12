import {
  detail3,
  detail4,
  list6,
  list7,
} from '../../shared/api/generated/sdk.gen'

export async function getManagedOrders(isAdmin: boolean) {
  const response = isAdmin
    ? await list7({
        query: { page: 1, pageSize: 100 },
        throwOnError: true,
      })
    : await list6({
        query: { page: 1, pageSize: 100 },
        throwOnError: true,
      })
  return response.data.data
}

export async function getManagedOrder(isAdmin: boolean, orderId: number) {
  const response = isAdmin
    ? await detail4({ path: { orderId }, throwOnError: true })
    : await detail3({ path: { orderId }, throwOnError: true })
  return response.data.data
}
