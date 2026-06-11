export function apiErrorMessage(error: unknown, fallback: string) {
  const response = (
    error as {
      response?: { data?: { message?: string } }
    }
  ).response
  return response?.data?.message || fallback
}
