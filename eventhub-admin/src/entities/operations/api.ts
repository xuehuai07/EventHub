import {
  getAdminOperationsDashboard,
  getAdminSalesTrend,
  getAdminTopActivities,
  getMerchantOperationsDashboard,
  getMerchantSalesTrend,
  getMerchantTopActivities,
  hideActivityReview,
  listAdminActivityReviews,
  listAdminOperationLogs,
  restoreActivityReview,
} from '../../shared/api/generated/sdk.gen'

export async function getOperationsDashboard(admin: boolean) {
  const response = admin
    ? await getAdminOperationsDashboard({ throwOnError: true })
    : await getMerchantOperationsDashboard({ throwOnError: true })
  return response.data.data
}

export async function getSalesTrend(admin: boolean) {
  const response = admin
    ? await getAdminSalesTrend({ throwOnError: true })
    : await getMerchantSalesTrend({ throwOnError: true })
  return response.data.data ?? []
}

export async function getTopActivities(admin: boolean) {
  const response = admin
    ? await getAdminTopActivities({
        query: { limit: 8 },
        throwOnError: true,
      })
    : await getMerchantTopActivities({
        query: { limit: 8 },
        throwOnError: true,
      })
  return response.data.data ?? []
}

export async function getOperationLogs(query?: {
  action?: string
  resourceType?: string
  page?: number
}) {
  return (
    await listAdminOperationLogs({
      query: { page: 1, pageSize: 20, ...query },
      throwOnError: true,
    })
  ).data.data
}

export async function getAdminReviews(query?: {
  status?: string
  keyword?: string
  page?: number
}) {
  return (
    await listAdminActivityReviews({
      query: { page: 1, pageSize: 20, ...query },
      throwOnError: true,
    })
  ).data.data
}

export async function hideReview(reviewId: number, reason: string) {
  return (
    await hideActivityReview({
      path: { reviewId },
      body: { reason },
      throwOnError: true,
    })
  ).data.data
}

export async function restoreReview(reviewId: number) {
  return (
    await restoreActivityReview({
      path: { reviewId },
      throwOnError: true,
    })
  ).data.data
}
