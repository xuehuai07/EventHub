# EventHub 架构与接口约束

## 版本

当前稳定约束版本：`0.4.1`（2026-06-12）。

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

订单模块使用以下键空间：

- `eventhub:seat:lock:{sessionId}:{sessionSeatId}`：固定座位临时锁。
- `eventhub:stock:locks:{ticketTypeId}`：无座票临时库存 Sorted Set。
- `eventhub:idempotent:{userId}:{scope}:{key}`：短期幂等进行中标记。

Redis 临时锁不能作为最终售出依据，MySQL 条件更新和唯一约束必须始终保留。

## 活动领域

- 活动、场馆、场次和票档数据由 `com.eventhub.activity` 领域维护。
- 活动状态固定为 `DRAFT`、`PENDING_REVIEW`、`PUBLISHED`、`REJECTED`、`OFF_SHELF`、`FINISHED`。
- 商家可以完整编辑 `DRAFT` 或 `REJECTED` 活动。
- 商家可以编辑 `PUBLISHED` 活动的封面、简介和详情，活动保持发布状态。
- 已发布活动的标题、分类、城市、场次和票档不得通过商家接口直接修改。
- 只有管理员可以将待审核活动发布、驳回或将已发布活动下架。
- 公开活动接口只能返回 `PUBLISHED` 状态的数据。
- 商家写操作必须同时校验角色、商家状态、员工状态和 `merchant_id` 数据归属。
- 已发布活动不允许直接修改影响审核、检索归类、售卖和履约的数据。
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
- 草稿修改、已发布展示内容修改、场次票档变更、提交审核、审核、驳回和下架后必须删除对应缓存。
- 缓存不是活动状态或库存的最终可信来源，MySQL 始终作为主存储。

## 订单领域

- 固定座位场次必须在保存票档后生成 `eh_session_seat` 快照。
- 固定座位的票档 `seat_grade` 必须覆盖所有有效场馆座位等级，未映射座位不得提交审核。
- Redis 只维护短期 `LOCKED` 状态；`eh_session_seat` 只持久化 `AVAILABLE`、`SOLD`、`DISABLED`。
- `eh_session_ticket_type.available_stock` 表示未被订单最终占用的库存，不扣除 Redis 临时锁。
- 创建订单必须使用 `Idempotency-Key`，并由 `(user_id, request_id)` 唯一约束最终防重。
- 订单状态固定为 `PENDING_PAYMENT`、`PAID`、`CANCELLED`、`EXPIRED`。
- 只有待支付订单允许支付、取消或超时关闭；取消与超时必须恢复库存。
- 用户只能访问自己的订单，商家只能访问所属商家订单，管理员可以读取平台订单。
- 订单创建和支付产生的异步事件必须与业务数据在同一事务写入 Outbox，禁止事务提交前直接依赖 RabbitMQ 发布成功。
- RabbitMQ 超时消息只负责触发，订单状态条件更新和 MySQL 库存仍是最终可信依据。
- 数据库订单超时扫描必须保留，作为消息延迟、丢失或 RabbitMQ 不可用时的兜底。
- 取消或过期订单恢复固定座位后，该座位必须允许被新订单重新购买；历史订单明细不得使用场次座位全局唯一约束阻止转售。

## 订单消息

RabbitMQ 使用以下交换机和队列：

```text
eventhub.order.events
eventhub.order.delay
eventhub.order.dlx

eventhub.order.timeout.delay.q
eventhub.order.timeout.q
eventhub.order.paid.q
eventhub.order.dead.q
```

- `ORDER_CREATED` 事件按支付截止时间设置消息 TTL，经死信路由进入订单超时队列。
- `ORDER_PAID` 事件进入支付队列并异步生成电子票。
- Outbox 发布使用 Publisher Confirm，发布失败必须保留事件并退避重试。
- 消费语义为至少一次，所有消费者必须使用 `(consumer_name, event_id)` 或等价数据库约束实现幂等。
- 消费失败只能有限重试，超过上限必须进入死信队列，禁止无限重新入队。

## 电子票

- 支付成功后按订单明细数量生成一人一票的 `eh_ticket` 记录。
- 票号必须使用不可预测随机值，不得直接暴露自增主键。
- `(order_item_id, unit_no)` 必须唯一，重复支付事件不得生成重复票。
- 当前票券状态仅为 `UNUSED`；核销状态、核销操作人和审计信息在后续票券阶段扩展。
- 本阶段不提供公开票券和核销 API。

## 订单接口

```text
GET    /api/sessions/{sessionId}/availability
POST   /api/seat-locks
GET    /api/seat-locks/{lockNo}
DELETE /api/seat-locks/{lockNo}

POST /api/orders
GET  /api/orders
GET  /api/orders/{orderId}
POST /api/orders/{orderId}/pay
POST /api/orders/{orderId}/cancel

GET /api/merchant/orders
GET /api/merchant/orders/{orderId}
GET /api/admin/orders
GET /api/admin/orders/{orderId}
```

## 模块边界

- Controller 只负责 HTTP 协议、校验和调用应用服务。
- 状态转换由独立领域规则维护，不散落在 Controller 或 Mapper。
- MyBatis Mapper 按命令、查询和子领域拆分。
- Redis Key 和序列化由缓存组件封装。
- 前端页面只组合 Feature，服务端数据由 TanStack Query 管理。
- OpenAPI 生成目录禁止手工修改。

## 媒体文件

- 商家活动封面上传接口为 `POST /api/merchant/uploads/activity-cover`。
- 活动封面公开读取接口为 `GET /api/media/activity-covers/{fileName}`。
- 上传文件只允许真实 JPG 或 PNG，最大 5 MB，最大尺寸 6000 × 6000。
- 客户端文件名不得作为服务端路径，存储文件名必须由服务端随机生成。
- 本地开发默认存储到 `.data/uploads/activity-covers`，可通过 `UPLOAD_ROOT` 覆盖。
- 业务数据只保存媒体 URL，不依赖本地文件实现；生产环境可替换为对象存储。
