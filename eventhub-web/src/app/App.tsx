import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import { HomePage } from '../pages/HomePage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { LoginPage } from '../pages/LoginPage'
import { RegisterPage } from '../pages/RegisterPage'
import './app.css'

const ActivityListPage = lazy(() =>
  import('../pages/ActivityListPage').then((module) => ({
    default: module.ActivityListPage,
  })),
)
const ActivityDetailPage = lazy(() =>
  import('../pages/ActivityDetailPage').then((module) => ({
    default: module.ActivityDetailPage,
  })),
)
const SeatSelectionPage = lazy(() =>
  import('../pages/SeatSelectionPage').then((module) => ({
    default: module.SeatSelectionPage,
  })),
)
const OrderConfirmationPage = lazy(() =>
  import('../pages/OrderConfirmationPage').then((module) => ({
    default: module.OrderConfirmationPage,
  })),
)
const OrderListPage = lazy(() =>
  import('../pages/OrderListPage').then((module) => ({
    default: module.OrderListPage,
  })),
)
const OrderDetailPage = lazy(() =>
  import('../pages/OrderDetailPage').then((module) => ({
    default: module.OrderDetailPage,
  })),
)
const TicketListPage = lazy(() =>
  import('../pages/TicketListPage').then((module) => ({
    default: module.TicketListPage,
  })),
)
const TicketDetailPage = lazy(() =>
  import('../pages/TicketDetailPage').then((module) => ({
    default: module.TicketDetailPage,
  })),
)
const NotificationPage = lazy(() =>
  import('../pages/NotificationPage').then((module) => ({
    default: module.NotificationPage,
  })),
)

export function App() {
  return (
    <Suspense fallback={<div className="route-loading">正在加载活动...</div>}>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/activities" element={<ActivityListPage />} />
        <Route
          path="/activities/:activityId"
          element={<ActivityDetailPage />}
        />
        <Route
          path="/sessions/:sessionId/tickets"
          element={<SeatSelectionPage />}
        />
        <Route path="/checkout/:lockNo" element={<OrderConfirmationPage />} />
        <Route path="/orders" element={<OrderListPage />} />
        <Route path="/orders/:orderId" element={<OrderDetailPage />} />
        <Route path="/tickets" element={<TicketListPage />} />
        <Route path="/tickets/:ticketId" element={<TicketDetailPage />} />
        <Route path="/notifications" element={<NotificationPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  )
}
