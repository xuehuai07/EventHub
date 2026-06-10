# EventHub 版本管理

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
