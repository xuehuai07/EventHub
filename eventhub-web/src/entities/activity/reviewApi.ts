import {
  deleteMyActivityReview,
  getActivityReviewSummary,
  getMyActivityReview,
  listPublicActivityReviews,
  saveMyActivityReview,
} from '../../shared/api/generated/sdk.gen'
import type { ActivityReviewRequest } from '../../shared/api/generated/types.gen'

export async function getReviews(activityId: number, page = 1) {
  return (
    await listPublicActivityReviews({
      path: { activityId },
      query: { page, pageSize: 10 },
      throwOnError: true,
    })
  ).data.data
}

export async function getReviewSummary(activityId: number) {
  return (
    await getActivityReviewSummary({
      path: { activityId },
      throwOnError: true,
    })
  ).data.data
}

export async function getMyReview(activityId: number) {
  return (
    await getMyActivityReview({
      path: { activityId },
      throwOnError: true,
    })
  ).data.data
}

export async function saveMyReview(
  activityId: number,
  body: ActivityReviewRequest,
) {
  return (
    await saveMyActivityReview({
      path: { activityId },
      body,
      throwOnError: true,
    })
  ).data.data
}

export async function deleteMyReview(activityId: number) {
  await deleteMyActivityReview({
    path: { activityId },
    throwOnError: true,
  })
}
