import {
  logs,
  logs1,
  preview,
  verify,
} from '../../shared/api/generated/sdk.gen'
import type { VerificationRequest } from '../../shared/api/generated/types.gen'

export async function previewTicket(body: VerificationRequest) {
  return (await preview({ body, throwOnError: true })).data.data
}

export async function verifyTicket(body: VerificationRequest) {
  return (await verify({ body, throwOnError: true })).data.data
}

export async function getVerificationLogs(admin: boolean) {
  const response = admin
    ? await logs1({ query: { page: 1, pageSize: 100 }, throwOnError: true })
    : await logs({ query: { page: 1, pageSize: 100 }, throwOnError: true })
  return response.data.data
}
