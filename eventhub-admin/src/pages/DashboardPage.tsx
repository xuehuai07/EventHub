import {
  CheckCircleFilled,
  ClockCircleOutlined,
  DatabaseOutlined,
  DeploymentUnitOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Progress, Tag } from 'antd'
import { getSystemStatus } from '../shared/api/system'

const foundationItems = [
  'Java 21 与 Spring Boot 3.5',
  'MySQL 与 Redis 容器环境',
  '统一 API 响应协议',
  'OpenAPI 客户端代码生成',
  '独立 React 用户端与管理端',
]

export function DashboardPage() {
  const statusQuery = useQuery({
    queryKey: ['system-status'],
    queryFn: getSystemStatus,
    refetchInterval: 30_000,
  })
  const apiOnline = statusQuery.data?.data.status === 'UP'

  return (
    <div className="dashboard">
      <section className="dashboard-intro">
        <div>
          <span className="section-label">环境概览</span>
          <h1>核心业务开发环境已就绪</h1>
          <p>
            工程结构、接口契约和本地基础设施均已完成。下一阶段将实现用户认证、
            角色权限和商家身份体系。
          </p>
        </div>
        <div className="release-card">
          <span>当前里程碑</span>
          <strong>基础工程</strong>
          <Tag bordered={false} color="success">
            已完成
          </Tag>
        </div>
      </section>

      <section className="metric-grid">
        <article className="metric-card metric-dark">
          <span className="metric-icon">
            <DeploymentUnitOutlined />
          </span>
          <div>
            <small>后端服务</small>
            <strong>{apiOnline ? '运行正常' : '暂不可用'}</strong>
            <p>
              {statusQuery.isPending
                ? '正在检查服务'
                : statusQuery.isError
                  ? '代理未收到响应'
                  : statusQuery.data.data.service}
            </p>
          </div>
        </article>

        <article className="metric-card">
          <span className="metric-icon">
            <DatabaseOutlined />
          </span>
          <div>
            <small>本地基础设施</small>
            <strong>2 个服务</strong>
            <p>MySQL 8.4 · Redis 7.4</p>
          </div>
        </article>

        <article className="metric-card">
          <span className="metric-icon">
            <ClockCircleOutlined />
          </span>
          <div>
            <small>下一里程碑</small>
            <strong>身份与权限</strong>
            <p>用户、角色、商家与登录令牌</p>
          </div>
        </article>
      </section>

      <section className="dashboard-columns">
        <article className="foundation-panel">
          <div className="panel-heading">
            <div>
              <span className="section-label">阶段 0 检查项</span>
              <h2>工程基础能力</h2>
            </div>
            <Progress type="circle" percent={100} size={62} />
          </div>
          <div className="foundation-list">
            {foundationItems.map((item) => (
              <div key={item}>
                <CheckCircleFilled />
                <span>{item}</span>
              </div>
            ))}
          </div>
        </article>

        <article className="contract-panel">
          <span className="section-label">实时契约</span>
          <h2>API 连接信息</h2>
          <dl>
            <div>
              <dt>状态</dt>
              <dd>
                <span className={apiOnline ? 'live-dot' : 'live-dot is-off'} />
                {apiOnline ? '健康' : '检查中'}
              </dd>
            </div>
            <div>
              <dt>接口地址</dt>
              <dd>/api/system/status</dd>
            </div>
            <div>
              <dt>请求编号</dt>
              <dd>{statusQuery.data?.requestId ?? '等待响应'}</dd>
            </div>
          </dl>
        </article>
      </section>
    </div>
  )
}
