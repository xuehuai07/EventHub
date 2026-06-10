import { client } from './generated/client.gen'
import type { ApiResponseAuthResponse } from './generated/types.gen'
import { useAuthStore } from '../auth/authStore'

const clientType = 'ADMIN_WEB'
const csrfCookieName = 'eventhub_admin_csrf'
let refreshPromise: Promise<string> | null = null

client.setConfig({
  baseURL: import.meta.env.VITE_API_ORIGIN || '',
  timeout: 10_000,
  withCredentials: true,
})

client.instance.interceptors.request.use((config) => {
  config.headers.set('X-Request-Id', crypto.randomUUID())
  const accessToken = useAuthStore.getState().accessToken
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

client.instance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config as typeof error.config & { _retry?: boolean }
    const isAuthEndpoint =
      config?.url?.includes('/api/auth/login') ||
      config?.url?.includes('/api/auth/refresh')

    if (
      error.response?.status !== 401 ||
      !config ||
      config._retry ||
      isAuthEndpoint
    ) {
      throw error
    }

    config._retry = true
    try {
      const accessToken = await refreshAccessToken()
      config.headers.set('Authorization', `Bearer ${accessToken}`)
      return client.instance.request(config)
    } catch (refreshError) {
      useAuthStore.getState().clearSession()
      throw refreshError
    }
  },
)

export function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = client.instance
      .post<ApiResponseAuthResponse>('/api/auth/refresh', null, {
        headers: {
          'X-Client-Type': clientType,
          'X-CSRF-Token': readCookie(csrfCookieName) || '',
        },
      })
      .then((response) => {
        const auth = response.data.data
        if (!auth?.accessToken || !auth.user) {
          throw new Error('刷新登录状态失败')
        }
        useAuthStore.getState().setSession(auth.accessToken, auth.user)
        return auth.accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

export function readCookie(name: string) {
  const prefix = `${name}=`
  const cookie = document.cookie
    .split('; ')
    .find((item) => item.startsWith(prefix))
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : undefined
}

export { client as http }
