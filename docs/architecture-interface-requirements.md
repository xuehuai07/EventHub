# EventHub 架构与接口约束

## 版本

当前稳定约束版本：`0.7.0`（2026-06-12）。

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
- 票券状态固定为 `UNUSED`、`USED`、`CANCELLED`。
- 动态二维码使用独立 HMAC 密钥签名，默认有效期 60 秒；签名密钥不得与 JWT 密钥共用。
- 永久票号只作为人工核销兜底，不得使用数据库自增主键作为票码。
- 商家只能预览和核销所属商家的票券；管理员只能查询平台核销记录。
- 首次核销必须使用 `WHERE status = 'UNUSED'` 的 MySQL 条件更新保证原子性。
- 首次和重复核销均写入 `eh_ticket_verification_log`，不得记录 Access Token、二维码密钥或完整请求头。

## 票券与核销接口

```text
GET  /api/tickets
GET  /api/tickets/{ticketId}
POST /api/tickets/{ticketId}/credential
GET  /api/orders/{orderId}/tickets

POST /api/merchant/ticket-verifications/preview
POST /api/merchant/ticket-verifications
GET  /api/merchant/ticket-verifications
GET  /api/admin/ticket-verifications
```

## 站内通知与 WebSocket

- 通知类型固定包含 `ORDER_PAID`、`ORDER_CANCELLED`、`ORDER_EXPIRED`、`TICKET_ISSUED`。
- 通知创建与对应业务状态变化在同一 MySQL 事务中完成。
- WebSocket 推送在事务提交后执行，推送失败不得回滚订单、票券或通知。
- STOMP 端点为 `/ws`，`CONNECT` 必须携带有效 Access Token。
- 用户只允许订阅自己的 `/user/queue/notifications` 和 `/user/queue/status`。
- WebSocket 只负责及时通知；页面初始加载、重连恢复和最终状态必须通过 HTTP 查询。

```text
GET  /api/notifications
GET  /api/notifications/unread-count
POST /api/notifications/{notificationId}/read
POST /api/notifications/read-all
```

## AI 智能助手

- AI 助手模块固定为 `com.eventhub.assistant`，只允许已登录的 `USER_WEB` 普通用户访问。
- 会话和消息分别保存于 `eh_ai_conversation`、`eh_ai_message`；会话归属必须使用当前认证用户 ID 校验。
- 删除会话时数据库级联硬删除消息；未完整生成的助手回复不得保存。
- DeepSeek Key 只允许通过后端 `DEEPSEEK_API_KEY` 环境变量注入，不得进入前端、数据库、日志或 Git。
- 模型不得直接访问数据库、任意接口或任意 URL，只能调用后端注册的固定只读工具。
- 首版工具固定为活动搜索、活动可售场次、本人已支付订单、本人订单票券和本人票券列表。
- 订单和票券工具必须在 SQL 或应用服务入口使用当前用户 ID 限制归属，不接受模型提供用户 ID。
- 发往模型的数据不得包含手机号、密码、Token、永久票号或二维码凭证。
- 工具结果按不可信数据处理；模型生成的 URL 不作为可点击链接，资源链接只能由后端按站内路由生成。
- 每条输入最多 2000 字，只发送最近 20 轮，最多执行 3 轮工具调用，总超时 60 秒。
- 每个用户同时只允许一个生成流，Redis 键固定使用 `eventhub:assistant:stream:{userId}`。
- SSE 事件固定为 `ack`、`delta`、`resources`、`done`、`error`。

```text
GET    /api/assistant/conversations
POST   /api/assistant/conversations
PUT    /api/assistant/conversations/{conversationId}
DELETE /api/assistant/conversations/{conversationId}
GET    /api/assistant/conversations/{conversationId}/messages
POST   /api/assistant/conversations/{conversationId}/messages/stream
```

## 活动收藏与评价

- 活动收藏由 `eh_activity_favorite` 维护，`(user_id, activity_id)` 必须唯一。
- 只有已登录普通用户可以收藏 `PUBLISHED` 活动；取消收藏按幂等成功处理。
- 已下架活动不允许新增收藏，历史收藏记录可以继续查询但不得进入购票流程。
- 活动评价由 `eh_activity_review` 维护，每个用户对同一活动最多一条。
- 用户必须存在该活动的本人 `PAID` 订单，且对应场次已经开始，才能创建或更新评价。
- 评价评分固定为 1 至 5 分整数，正文为最长 1000 字纯文本。
- 评价状态固定为 `PUBLISHED`、`HIDDEN`。
- 被管理员隐藏的评价不进入公开列表和评分聚合，用户不得通过编辑自行恢复。
- 评价隐藏和恢复必须失效对应公开活动详情缓存。

```text
GET    /api/activity-favorites
PUT    /api/activity-favorites/{activityId}
DELETE /api/activity-favorites/{activityId}
GET    /api/activity-favorites/{activityId}/status

GET    /api/activities/{activityId}/reviews
GET    /api/activities/{activityId}/review-summary
GET    /api/activities/{activityId}/my-review
PUT    /api/activities/{activityId}/my-review
DELETE /api/activities/{activityId}/my-review

GET  /api/admin/activity-reviews
POST /api/admin/activity-reviews/{reviewId}/hide
POST /api/admin/activity-reviews/{reviewId}/restore
```

## 运营统计与操作审计

- 平台统计覆盖全平台，商家统计必须在 SQL 中使用当前认证商家的 `merchant_id` 限制范围。
- 成交额、已支付订单和售出票数以 `eh_ticket_order.status = 'PAID'` 为准。
- 已核销票数以 `eh_ticket.status = 'USED'` 为准。
- 销售趋势日期范围最多 366 天，默认最近 30 天。
- `eh_operation_log` 记录关键商家和管理员写操作，不建立指向业务资源的外键。
- 成功审计记录必须与对应业务写操作处于同一事务。
- 审计摘要只允许使用业务字段白名单，不得记录密码、Token、Cookie、二维码凭证、完整请求头或完整请求体。

```text
GET /api/admin/dashboard/operations
GET /api/admin/dashboard/sales-trend
GET /api/admin/dashboard/top-activities

GET /api/merchant/dashboard/operations
GET /api/merchant/dashboard/sales-trend
GET /api/merchant/dashboard/top-activities

GET /api/admin/operation-logs
```

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
