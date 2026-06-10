import {
  activities,
  categories,
  detail2,
} from '../../shared/api/generated/sdk.gen'
import type { ActivitiesData } from '../../shared/api/generated/types.gen'

export async function getActivityCategories() {
  return (await categories({ throwOnError: true })).data.data ?? []
}

export async function getActivities(query?: ActivitiesData['query']) {
  return (
    await activities({
      query: { page: 1, pageSize: 12, ...query },
      throwOnError: true,
    })
  ).data.data
}

export async function getActivityDetail(activityId: number) {
  return (await detail2({ path: { activityId }, throwOnError: true })).data.data
}
