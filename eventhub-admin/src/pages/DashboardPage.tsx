import {
  CheckCircleFilled,
  ClockCircleOutlined,
  DatabaseOutlined,
  DeploymentUnitOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Progress, Tag } from 'antd'
import { summary } from '../shared/api/generated/sdk.gen'
import { getSystemStatus } from '../shared/api/system'

const identityItems = [
  '商家与场馆数据归属',
  '活动草稿与状态机',
  '场次、票档与固定座位',
  '管理员审核与发布',
  '用户端公开活动浏览',
]

export function DashboardPage() {
  const statusQuery = useQuery({
    queryKey: ['system-status'],
    queryFn: getSystemStatus,
    refetchInterval: 30_000,
  })
  const apiOnline = statusQuery.data?.data.status === 'UP'
  const summaryQuery = useQuery({
    queryKey: ['activity-dashboard-summary'],
    queryFn: async () => (await summary({ throwOnError: true })).data.data,
  })
  const metrics = summaryQuery.data

  return (
    <div className="dashboard">
      <section className="dashboard-intro">
        <div>
          <span className="section-label">环境概览</span>
          <h1>活动发布链路已接通</h1>
          <p>
            商家可以配置场馆、场次和票档，管理员完成审核后，活动将实时出现在用户端。
          </p>
        </div>
        <div className="release-card">
          <span>当前里程碑</span>
          <strong>活动与场次</strong>
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
            <strong>{metrics?.merchantCount ?? 0} 个商家</strong>
            <p>{metrics?.publishedCount ?? 0} 个活动已发布</p>
          </div>
        </article>

        <article className="metric-card">
          <span className="metric-icon">
            <ClockCircleOutlined />
          </span>
          <div>
            <small>下一里程碑</small>
            <strong>{metrics?.upcomingSessionCount ?? 0} 个近期场次</strong>
            <p>{metrics?.pendingReviewCount ?? 0} 个活动等待审核</p>
          </div>
        </article>
      </section>

      <section className="dashboard-columns">
        <article className="foundation-panel">
          <div className="panel-heading">
            <div>
              <span className="section-label">阶段 2 检查项</span>
              <h2>活动业务能力</h2>
            </div>
            <Progress type="circle" percent={100} size={62} />
          </div>
          <div className="foundation-list">
            {identityItems.map((item) => (
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
