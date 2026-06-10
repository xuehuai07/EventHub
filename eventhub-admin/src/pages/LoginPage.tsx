import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { Button, Form, Input } from 'antd'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { loginAdmin } from '../shared/auth/authApi'
import { useAuthStore } from '../shared/auth/authStore'

interface LoginValues {
  identifier: string
  password: string
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const status = useAuthStore((state) => state.status)
  const [form] = Form.useForm<LoginValues>()

  if (status === 'authenticated') {
    return <Navigate to="/" replace />
  }

  async function handleSubmit(values: LoginValues) {
    try {
      await loginAdmin(values.identifier, values.password)
      const destination =
        (location.state as { from?: string } | null)?.from || '/'
      navigate(destination, { replace: true })
    } catch {
      form.setFields([
        {
          name: 'password',
          errors: ['登录失败，请确认账号具备商家或管理员权限。'],
        },
      ])
    }
  }

  return (
    <main className="admin-login-page">
      <section className="admin-login-copy">
        <span className="admin-login-mark">E</span>
        <div>
          <small>EventHub Operations</small>
          <h1>让城市活动运营更清晰</h1>
          <p>面向商家与平台管理员的活动、订单、核销和审核工作台。</p>
        </div>
      </section>
      <section className="admin-login-card">
        <span className="section-label">安全登录</span>
        <h2>进入运营管理后台</h2>
        <p>普通用户账号不能登录此客户端。</p>
        <Form
          form={form}
          layout="vertical"
          requiredMark={false}
          onFinish={handleSubmit}
        >
          <Form.Item
            label="账号"
            name="identifier"
            rules={[{ required: true, message: '请输入用户名或手机号' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名或手机号"
              autoComplete="username"
              size="large"
            />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入密码"
              autoComplete="current-password"
              size="large"
            />
          </Form.Item>
          <Button type="primary" htmlType="submit" size="large" block>
            登录管理后台
          </Button>
        </Form>
      </section>
    </main>
  )
}
