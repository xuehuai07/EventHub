import { uploadActivityCover } from '../../shared/api/generated/sdk.gen'

export async function uploadCover(file: File) {
  return (
    await uploadActivityCover({
      body: { file },
      throwOnError: true,
    })
  ).data.data
}
