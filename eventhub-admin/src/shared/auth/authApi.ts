import { login, logout } from '../api/generated/sdk.gen'
import { readCookie } from '../api/http'
import { useAuthStore } from './authStore'

export async function loginAdmin(identifier: string, password: string) {
  const response = await login({
    body: { identifier, password, clientType: 'ADMIN_WEB' },
    throwOnError: true,
  })
  const auth = response.data.data
  if (!auth?.accessToken || !auth.user) {
    throw new Error('登录响应缺少必要数据')
  }
  useAuthStore.getState().setSession(auth.accessToken, auth.user)
  return auth.user
}

export async function logoutAdmin() {
  try {
    await logout({
      headers: {
        'X-Client-Type': 'ADMIN_WEB',
        'X-CSRF-Token': readCookie('eventhub_admin_csrf') || '',
      },
      throwOnError: true,
    })
  } finally {
    useAuthStore.getState().clearSession()
  }
}
