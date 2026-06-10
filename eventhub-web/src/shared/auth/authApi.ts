import { login, logout, register } from '../api/generated/sdk.gen'
import type { AuthUserView, RegisterRequest } from '../api/generated/types.gen'
import { readCookie } from '../api/http'
import { useAuthStore } from './authStore'

export async function loginUser(identifier: string, password: string) {
  const response = await login({
    body: { identifier, password, clientType: 'USER_WEB' },
    throwOnError: true,
  })
  const auth = response.data.data
  if (!auth?.accessToken || !auth.user) {
    throw new Error('登录响应缺少必要数据')
  }
  useAuthStore.getState().setSession(auth.accessToken, auth.user)
  return auth.user
}

export async function registerUser(
  input: Omit<RegisterRequest, 'identifierPresent'>,
) {
  const response = await register({ body: input, throwOnError: true })
  if (!response.data.data) {
    throw new Error('注册响应缺少用户数据')
  }
  return response.data.data
}

export async function logoutUser() {
  try {
    await logout({
      headers: {
        'X-Client-Type': 'USER_WEB',
        'X-CSRF-Token': readCookie('eventhub_user_csrf') || '',
      },
      throwOnError: true,
    })
  } finally {
    useAuthStore.getState().clearSession()
  }
}

export function userLabel(user: AuthUserView) {
  return user.displayName || user.username || user.phone || 'EventHub 用户'
}
