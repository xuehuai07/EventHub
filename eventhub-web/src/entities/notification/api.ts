import {
  list5,
  markAllRead,
  markRead,
  unreadCount,
} from '../../shared/api/generated/sdk.gen'

export async function getNotifications() {
  return (
    await list5({ query: { page: 1, pageSize: 100 }, throwOnError: true })
  ).data.data
}

export async function getUnreadCount() {
  return (await unreadCount({ throwOnError: true })).data.data
}

export async function readNotification(notificationId: number) {
  await markRead({ path: { notificationId }, throwOnError: true })
}

export async function readAllNotifications() {
  await markAllRead({ throwOnError: true })
}
