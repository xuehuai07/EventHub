import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <main className="not-found">
      <div>
        <h1>404</h1>
        <p>你访问的页面不存在或已经下线。</p>
        <Link className="primary-action" to="/">
          返回首页
        </Link>
      </div>
    </main>
  )
}
