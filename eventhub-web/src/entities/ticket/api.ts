import {
  credential,
  detail1,
  list4,
  orderTickets,
} from '../../shared/api/generated/sdk.gen'

export async function getTickets(status?: 'UNUSED' | 'USED' | 'CANCELLED') {
  return (
    await list4({
      query: { status, page: 1, pageSize: 100 },
      throwOnError: true,
    })
  ).data.data
}

export async function getTicket(ticketId: number) {
  return (await detail1({ path: { ticketId }, throwOnError: true })).data.data
}

export async function getTicketCredential(ticketId: number) {
  return (await credential({ path: { ticketId }, throwOnError: true })).data
    .data
}

export async function getOrderTickets(orderId: number) {
  return (await orderTickets({ path: { orderId }, throwOnError: true })).data
    .data
}
