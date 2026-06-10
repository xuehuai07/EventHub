# EventHub 版本管理

## 0.2.0 - 2026-06-10

阶段 2“活动、场馆与场次”完成。

### 公共接口

- 新增公开活动分类、活动分页筛选和活动详情接口。
- 新增商家场馆、固定座位、活动草稿、场次票档和提交审核接口。
- 新增管理员商家管理、活动审核、驳回、下架和运营概览接口。

### 数据结构

- 新增活动分类、标签、场馆、场馆座位、活动、场次、票档和场次座位表。
- 活动状态固定为 `DRAFT`、`PENDING_REVIEW`、`PUBLISHED`、`REJECTED`、`OFF_SHELF`、`FINISHED`。
- 金额统一使用分为单位的整数，场馆和活动写操作使用商家数据归属。

### 架构

- 活动模块按 API、应用服务、领域规则、持久化和缓存拆分。
- 活动详情使用 Redis 缓存，写操作和状态变化主动失效。
- 用户端和管理端继续通过 OpenAPI 生成客户端访问后端。
- 前端业务页面使用路由懒加载，并按 Entity、Feature、Page 分层。

## 0.1.0 - 2026-06-10

阶段 1“用户认证与权限”完成。

### 公共接口

- 新增 `POST /api/auth/register`、`POST /api/auth/login`。
- 新增 `POST /api/auth/refresh`、`POST /api/auth/logout`。
- 新增 `GET /api/auth/me`。
- 新增 `GET /api/merchant/session`、`GET /api/admin/session`。

### 数据结构

- 新增用户、角色、权限及其关联表。
- 新增商家、商家员工及其关联结构。
- 基础角色固定为 `USER`、`MERCHANT`、`ADMIN`。

### 架构

- Access Token 使用短期 JWT，并仅保存在前端内存中。
- Refresh Token 使用 Redis 会话和 HttpOnly Cookie，支持轮换与撤销。
- 用户端与管理端使用独立客户端类型和 Cookie。
