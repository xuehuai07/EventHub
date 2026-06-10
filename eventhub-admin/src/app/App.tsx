import {
  AppstoreOutlined,
  AuditOutlined,
  CalendarOutlined,
  DashboardOutlined,
  SafetyCertificateOutlined,
  ShopOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { Layout, Menu, Tag } from 'antd'
import { lazy, Suspense } from 'react'
import { Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import './app.css'

const { Content, Header, Sider } = Layout
const DashboardPage = lazy(() =>
  import('../pages/DashboardPage').then((module) => ({
    default: module.DashboardPage,
  })),
)

const navigation = [
  { key: '/', icon: <DashboardOutlined />, label: '工作台' },
  { key: '/activities', icon: <CalendarOutlined />, label: '活动管理' },
  { key: '/merchants', icon: <ShopOutlined />, label: '商家管理' },
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
  const navigate = useNavigate()
  const location = useLocation()

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
          <Tag color="processing">阶段 0</Tag>
          <p>基础工程环境</p>
        </div>
      </Sider>

      <Layout>
        <Header className="admin-header">
          <div>
            <span className="header-kicker">2026 年 6 月 10 日 · 星期三</span>
            <strong>平台运营管理</strong>
          </div>
          <div className="operator-chip">
            <span>管</span>
            <div>
              <strong>演示管理员</strong>
              <small>平台管理员</small>
            </div>
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
                element={<PlaceholderPage title="活动管理" />}
              />
              <Route
                path="/merchants"
                element={<PlaceholderPage title="商家入驻审核" />}
              />
              <Route
                path="/orders"
                element={<PlaceholderPage title="订单运营" />}
              />
              <Route
                path="/verification"
                element={<PlaceholderPage title="票券核销" />}
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
