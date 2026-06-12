import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { AuthParticleWave } from '../features/auth/AuthParticleWave'
import { loginUser, registerUser } from '../shared/auth/authApi'
import { useAuthStore } from '../shared/auth/authStore'

export function RegisterPage() {
  const navigate = useNavigate()
  const status = useAuthStore((state) => state.status)
  const [username, setUsername] = useState('')
  const [phone, setPhone] = useState('')
  const [displayName, setDisplayName] = useState('')
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
      await registerUser({ username, phone, displayName, password })
      await loginUser(username || phone, password)
      navigate('/', { replace: true })
    } catch {
      setError('注册失败，请检查填写内容或更换用户名、手机号。')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <AuthParticleWave />
      <section className="auth-card auth-card-wide">
        <Link className="auth-brand" to="/">
          <span className="brand-mark">E</span>
          EventHub
        </Link>
        <span className="eyebrow">创建账号</span>
        <h1>加入城市活动社区</h1>
        <p>用户名和手机号至少填写一项。</p>
        <form onSubmit={handleSubmit}>
          <div className="form-grid">
            <label>
              用户名
              <input
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                placeholder="4 至 32 位字母、数字或下划线"
                autoComplete="username"
              />
            </label>
            <label>
              手机号
              <input
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                placeholder="中国大陆手机号"
                inputMode="tel"
                autoComplete="tel"
              />
            </label>
          </div>
          <label>
            昵称
            <input
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              placeholder="活动页面中展示的称呼"
              required
            />
          </label>
          <label>
            密码
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="至少 8 位"
              autoComplete="new-password"
              minLength={8}
              required
            />
          </label>
          {error && <div className="form-error">{error}</div>}
          <button
            className="primary-action"
            disabled={submitting || (!username && !phone)}
          >
            {submitting ? '正在创建账号...' : '注册并登录'}
          </button>
        </form>
        <p className="auth-switch">
          已有账号？<Link to="/login">返回登录</Link>
        </p>
      </section>
    </main>
  )
}
