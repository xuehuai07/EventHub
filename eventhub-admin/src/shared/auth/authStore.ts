import { create } from 'zustand'
import type { AuthUserView } from '../api/generated/types.gen'

type AuthStatus = 'bootstrapping' | 'authenticated' | 'anonymous'

interface AuthState {
  accessToken: string | null
  user: AuthUserView | null
  status: AuthStatus
  setSession: (accessToken: string, user: AuthUserView) => void
  clearSession: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  status: 'bootstrapping',
  setSession: (accessToken, user) =>
    set({ accessToken, user, status: 'authenticated' }),
  clearSession: () =>
    set({ accessToken: null, user: null, status: 'anonymous' }),
}))
