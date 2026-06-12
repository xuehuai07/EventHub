import {
  favoriteActivity,
  getMyActivityFavoriteStatus,
  listMyActivityFavorites,
  unfavoriteActivity,
} from '../../shared/api/generated/sdk.gen'

export async function getFavorites(page = 1) {
  return (
    await listMyActivityFavorites({
      query: { page, pageSize: 12 },
      throwOnError: true,
    })
  ).data.data
}

export async function getFavoriteStatus(activityId: number) {
  return (
    await getMyActivityFavoriteStatus({
      path: { activityId },
      throwOnError: true,
    })
  ).data.data
}

export async function setFavorite(activityId: number, favorited: boolean) {
  const response = favorited
    ? await unfavoriteActivity({
        path: { activityId },
        throwOnError: true,
      })
    : await favoriteActivity({
        path: { activityId },
        throwOnError: true,
      })
  return response.data.data
}
