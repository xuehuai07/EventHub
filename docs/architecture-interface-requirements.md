# EventHub 架构与接口约束

## 版本

当前稳定约束版本：`0.1.0`（2026-06-10）。

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
