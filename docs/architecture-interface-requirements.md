# EventHub 架构与接口约束

## 版本

当前稳定约束版本：`0.2.0`（2026-06-10）。

## 身份与会话

- 用户身份由 `eh_user`、`eh_role`、`eh_permission` 及关联表维护。
- 商家归属由 `eh_merchant`、`eh_merchant_staff` 维护。
- 角色编码固定为 `USER`、`MERCHANT`、`ADMIN`，变更需同步评估接口兼容性。
- 密码必须使用 BCrypt 散列，不得保存或记录明文密码。
- Access Token 为短期 JWT，只允许保存在客户端内存中。
- Refresh Token 为不可预测随机值，服务端只在 Redis 保存摘要。
- Refresh Token 每次使用后必须轮换，退出时必须撤销。
- 用户端客户端类型为 `USER_WEB`，管理端客户端类型为 `ADMIN_WEB`。
- Refresh 和 Logout 必须校验 CSRF 请求头、Cookie 与服务端会话摘要。

## 公共认证接口

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/auth/me
GET  /api/merchant/session
GET  /api/admin/session
```

- `/api/merchant/session` 只允许 `MERCHANT` 或 `ADMIN`。
- `/api/admin/session` 只允许 `ADMIN`。
- 公共接口响应继续使用项目统一 API 响应结构。

## Redis 键空间

- `eventhub:auth:refresh:{tokenId}`：Refresh Token 会话。
- `eventhub:auth:login-failure:{identifier}:{ip}`：登录失败计数。
- `eventhub:auth:login-lock:{identifier}:{ip}`：登录锁定状态。

认证模块新增键必须继续使用 `eventhub:auth:` 前缀。

## 活动领域

- 活动、场馆、场次和票档数据由 `com.eventhub.activity` 领域维护。
- 活动状态固定为 `DRAFT`、`PENDING_REVIEW`、`PUBLISHED`、`REJECTED`、`OFF_SHELF`、`FINISHED`。
- 商家只能编辑 `DRAFT` 或 `REJECTED` 活动。
- 只有管理员可以将待审核活动发布、驳回或将已发布活动下架。
- 公开活动接口只能返回 `PUBLISHED` 状态的数据。
- 商家写操作必须同时校验角色、商家状态、员工状态和 `merchant_id` 数据归属。
- 已发布活动不允许直接修改影响售卖的数据。
- 票档金额使用分为单位的整数，禁止使用浮点数作为业务金额。

## 活动接口

```text
GET  /api/activity-categories
GET  /api/activities
GET  /api/activities/{activityId}

GET  /api/merchant/venues
POST /api/merchant/venues
PUT  /api/merchant/venues/{venueId}
PUT  /api/merchant/venues/{venueId}/seats

GET  /api/merchant/activities
POST /api/merchant/activities
GET  /api/merchant/activities/{activityId}
PUT  /api/merchant/activities/{activityId}
POST /api/merchant/activities/{activityId}/sessions
PUT  /api/merchant/activities/{activityId}/sessions/{sessionId}
DELETE /api/merchant/activities/{activityId}/sessions/{sessionId}
POST /api/merchant/activities/{activityId}/submit

GET  /api/admin/activities/reviews
GET  /api/admin/activities/{activityId}
POST /api/admin/activities/{activityId}/approve
POST /api/admin/activities/{activityId}/reject
POST /api/admin/activities/{activityId}/off-shelf
```

## 活动缓存

- `eventhub:activity:detail:{activityId}` 保存已发布活动详情，默认有效期 10 分钟。
- 草稿修改、场次票档变更、提交审核、审核、驳回和下架后必须删除对应缓存。
- 缓存不是活动状态或库存的最终可信来源，MySQL 始终作为主存储。

## 模块边界

- Controller 只负责 HTTP 协议、校验和调用应用服务。
- 状态转换由独立领域规则维护，不散落在 Controller 或 Mapper。
- MyBatis Mapper 按命令、查询和子领域拆分。
- Redis Key 和序列化由缓存组件封装。
- 前端页面只组合 Feature，服务端数据由 TanStack Query 管理。
- OpenAPI 生成目录禁止手工修改。
