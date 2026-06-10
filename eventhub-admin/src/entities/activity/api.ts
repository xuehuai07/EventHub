import {
  approve,
  categories,
  create1,
  createSession,
  detail1,
  list1,
  pending,
  reject,
  submit,
} from '../../shared/api/generated/sdk.gen'
import type {
  ActivityRequest,
  ReviewRequest,
  SessionRequest,
} from '../../shared/api/generated/types.gen'

export async function getCategories() {
  return (await categories({ throwOnError: true })).data.data ?? []
}

export async function getMerchantActivities() {
  return (
    await list1({ query: { page: 1, pageSize: 100 }, throwOnError: true })
  ).data.data
}

export async function createActivity(body: ActivityRequest) {
  return (await create1({ body, throwOnError: true })).data.data
}

export async function addActivitySession(
  activityId: number,
  body: SessionRequest,
) {
  return (
    await createSession({
      path: { activityId },
      body,
      throwOnError: true,
    })
  ).data.data
}

export async function submitActivity(activityId: number) {
  return (await submit({ path: { activityId }, throwOnError: true })).data.data
}

export async function getPendingReviews() {
  return (
    await pending({ query: { page: 1, pageSize: 100 }, throwOnError: true })
  ).data.data
}

export async function getReviewDetail(activityId: number) {
  return (await detail1({ path: { activityId }, throwOnError: true })).data.data
}

export async function approveActivity(activityId: number) {
  return (await approve({ path: { activityId }, throwOnError: true })).data.data
}

export async function rejectActivity(activityId: number, body: ReviewRequest) {
  return (await reject({ path: { activityId }, body, throwOnError: true })).data
    .data
}
