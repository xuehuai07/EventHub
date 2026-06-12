import {
  AppstoreOutlined,
  AuditOutlined,
  CalendarOutlined,
  DashboardOutlined,
  SafetyCertificateOutlined,
  BankOutlined,
  ShopOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { Layout, Menu, Tag } from 'antd'
import { lazy, Suspense } from 'react'
import {
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from 'react-router-dom'
import { LoginPage } from '../pages/LoginPage'
import { logoutAdmin } from '../shared/auth/authApi'
import { useAuthStore } from '../shared/auth/authStore'
import './app.css'

const { Content, Header, Sider } = Layout
const DashboardPage = lazy(() =>
  import('../pages/DashboardPage').then((module) => ({
    default: module.DashboardPage,
  })),
)
const ActivityManagementPage = lazy(() =>
  import('../pages/ActivityManagementPage').then((module) => ({
    default: module.ActivityManagementPage,
  })),
)
const ActivityReviewPage = lazy(() =>
  import('../pages/ActivityReviewPage').then((module) => ({
    default: module.ActivityReviewPage,
  })),
)
const MerchantManagementPage = lazy(() =>
  import('../pages/MerchantManagementPage').then((module) => ({
    default: module.MerchantManagementPage,
  })),
)
const VenueManagementPage = lazy(() =>
  import('../pages/VenueManagementPage').then((module) => ({
    default: module.VenueManagementPage,
  })),
)
const OrderManagementPage = lazy(() =>
  import('../pages/OrderManagementPage').then((module) => ({
    default: module.OrderManagementPage,
  })),
)
const TicketVerificationPage = lazy(() =>
  import('../pages/TicketVerificationPage').then((module) => ({
    default: module.TicketVerificationPage,
  })),
)

const commonNavigation = [
  { key: '/', icon: <DashboardOutlined />, label: '工作台' },
  { key: '/orders', icon: <AppstoreOutlined />, label: '订单管理' },
  {
    key: '/verification',
    icon: <SafetyCertificateOutlined />,
    label: '票券核销',
  },
  { key: '/users', icon: <TeamOutlined />, label: '用户与权限' },
  { key: '/audit', icon: <AuditOutlined />, label: '操作审计' },
]

function PlaceholderPage({ title }: { title: string }) {
  return (
    <section className="placeholder-panel">
      <span>功能模块预留</span>
      <h1>{title}</h1>
      <p>该页面结构已准备就绪，将在后续业务阶段完成具体功能。</p>
    </section>
  )
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/*" element={<ProtectedAdminWorkspace />} />
    </Routes>
  )
}

function ProtectedAdminWorkspace() {
  const location = useLocation()
  const status = useAuthStore((state) => state.status)
  const user = useAuthStore((state) => state.user)
  const allowed =
    user?.roles?.includes('ADMIN') || user?.roles?.includes('MERCHANT')

  if (status !== 'authenticated' || !allowed) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: `${location.pathname}${location.search}` }}
      />
    )
  }

  return <AdminWorkspace />
}

function AdminWorkspace() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthStore((state) => state.user)
  const roleLabel = user?.roles?.includes('ADMIN') ? '平台管理员' : '商家员工'
  const isAdmin = user?.roles?.includes('ADMIN')
  const navigation = [
    ...commonNavigation.slice(0, 1),
    {
      key: '/activities',
      icon: <CalendarOutlined />,
      label: isAdmin ? '活动审核' : '活动管理',
    },
    ...(isAdmin
      ? [{ key: '/merchants', icon: <ShopOutlined />, label: '商家管理' }]
      : [{ key: '/venues', icon: <BankOutlined />, label: '场馆管理' }]),
    ...commonNavigation.slice(1),
  ]

  return (
    <Layout className="admin-shell">
      <Sider className="admin-sider" width={248} breakpoint="lg">
        <div className="admin-brand">
          <span className="admin-brand-mark">E</span>
          <div>
            <strong>EventHub</strong>
            <small>运营管理后台</small>
          </div>
        </div>
        <Menu
          className="admin-menu"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={navigation}
          onClick={({ key }) => navigate(key)}
        />
        <div className="stage-badge">
          <Tag color="processing">阶段 5</Tag>
          <p>票券核销与通知</p>
        </div>
      </Sider>

      <Layout>
        <Header className="admin-header">
          <div>
            <span className="header-kicker">2026 年 6 月 10 日 · 星期三</span>
            <strong>平台运营管理</strong>
          </div>
          <div className="operator-chip">
            <span>{user?.displayName?.slice(0, 1) || '管'}</span>
            <div>
              <strong>{user?.displayName || user?.username}</strong>
              <small>{roleLabel}</small>
            </div>
            <button
              className="operator-logout"
              onClick={() => void logoutAdmin()}
            >
              退出
            </button>
          </div>
        </Header>

        <Content className="admin-content">
          <Suspense
            fallback={<div className="route-loading">正在加载模块...</div>}
          >
            <Routes>
              <Route path="/" element={<DashboardPage />} />
              <Route
                path="/activities"
                element={
                  isAdmin ? <ActivityReviewPage /> : <ActivityManagementPage />
                }
              />
              <Route
                path="/merchants"
                element={
                  isAdmin ? (
                    <MerchantManagementPage />
                  ) : (
                    <Navigate to="/" replace />
                  )
                }
              />
              <Route
                path="/venues"
                element={
                  isAdmin ? (
                    <Navigate to="/" replace />
                  ) : (
                    <VenueManagementPage />
                  )
                }
              />
              <Route path="/orders" element={<OrderManagementPage />} />
              <Route
                path="/verification"
                element={<TicketVerificationPage />}
              />
              <Route
                path="/users"
                element={<PlaceholderPage title="用户与权限管理" />}
              />
              <Route
                path="/audit"
                element={<PlaceholderPage title="操作审计" />}
              />
            </Routes>
          </Suspense>
        </Content>
      </Layout>
    </Layout>
  )
}
