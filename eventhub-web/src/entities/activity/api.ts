import {
  getPublicActivityDetail,
  listPublicActivities,
  listPublicActivityCategories,
} from '../../shared/api/generated/sdk.gen'
import type { ListPublicActivitiesData } from '../../shared/api/generated/types.gen'

export async function getActivityCategories() {
  return (
    (await listPublicActivityCategories({ throwOnError: true })).data.data ?? []
  )
}

export async function getActivities(query?: ListPublicActivitiesData['query']) {
  return (
    await listPublicActivities({
      query: { page: 1, pageSize: 12, ...query },
      throwOnError: true,
    })
  ).data.data
}

export async function getActivityDetail(activityId: number) {
  return (
    await getPublicActivityDetail({
      path: { activityId },
      throwOnError: true,
    })
  ).data.data
}
