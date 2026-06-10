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
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  )
}
