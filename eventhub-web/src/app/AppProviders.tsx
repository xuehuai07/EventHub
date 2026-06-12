import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { PropsWithChildren } from 'react'
import { BrowserRouter } from 'react-router-dom'
import { AiAssistantWidget } from '../features/assistant/AiAssistantWidget'
import { AuthBootstrap } from '../shared/auth/AuthBootstrap'
import { useAuthStore } from '../shared/auth/authStore'
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
          <RealtimeBridge>
            {children}
            <AssistantMount />
          </RealtimeBridge>
        </AuthBootstrap>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

function AssistantMount() {
  const userId = useAuthStore((state) => state.user?.id)
  return <AiAssistantWidget key={userId || 'anonymous'} />
}
