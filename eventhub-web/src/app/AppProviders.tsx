import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { PropsWithChildren } from 'react'
import { BrowserRouter } from 'react-router-dom'
import { AuthBootstrap } from '../shared/auth/AuthBootstrap'
import { RealtimeBridge } from '../shared/realtime/RealtimeBridge'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 30_000,
    },
  },
})

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthBootstrap>
          <RealtimeBridge>{children}</RealtimeBridge>
        </AuthBootstrap>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
