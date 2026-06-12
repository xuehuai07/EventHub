import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { AuthParticleWave } from '../features/auth/AuthParticleWave'
import { loginUser } from '../shared/auth/authApi'
import { useAuthStore } from '../shared/auth/authStore'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const status = useAuthStore((state) => state.status)
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (status === 'authenticated') {
    return <Navigate to="/" replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      await loginUser(identifier, password)
      const destination =
        (location.state as { from?: string } | null)?.from || '/'
      navigate(destination, { replace: true })
    } catch {
      setError('登录失败，请检查账号和密码。')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <AuthParticleWave />
      <section className="auth-card">
        <Link className="auth-brand" to="/">
          <span className="brand-mark">E</span>
          EventHub
        </Link>
        <span className="eyebrow">欢迎回来</span>
        <h1>登录后继续发现精彩</h1>
        <p>使用用户名或中国大陆手机号登录。</p>
        <form onSubmit={handleSubmit}>
          <label>
            账号
            <input
              value={identifier}
              onChange={(event) => setIdentifier(event.target.value)}
              placeholder="用户名或手机号"
              autoComplete="username"
              required
            />
          </label>
          <label>
            密码
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="请输入密码"
              autoComplete="current-password"
              required
            />
          </label>
          {error && <div className="form-error">{error}</div>}
          <button className="primary-action" disabled={submitting}>
            {submitting ? '正在登录...' : '登录'}
          </button>
        </form>
        <p className="auth-switch">
          还没有账号？<Link to="/register">立即注册</Link>
        </p>
      </section>
    </main>
  )
}
