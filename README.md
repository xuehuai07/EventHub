# EventHub

EventHub 是一个城市活动预约与票务平台，当前仓库统一管理三个独立工程：

- `eventhub-server`：Spring Boot 3.5、Java 21 后端
- `eventhub-web`：React 用户端，开发端口 `3000`
- `eventhub-admin`：React 管理端，开发端口 `3001`

## 本地启动

先启动 MySQL 和 Redis：

```powershell
docker compose up -d
docker compose ps
```

分别启动三个应用：

```powershell
cd eventhub-server
.\mvnw spring-boot:run
```

```powershell
cd eventhub-web
npm install
npm run dev
```

```powershell
cd eventhub-admin
npm install
npm run dev
```

本地访问地址：

- 用户端：http://localhost:3000
- 管理端：http://localhost:3001
- 后端健康检查：http://localhost:8080/actuator/health
- OpenAPI：http://localhost:8080/v3/api-docs
- Swagger UI：http://localhost:8080/swagger-ui.html

## 接口代码生成

后端运行后，在两个前端工程内分别执行：

```powershell
npm run api:generate
```

生成代码位于 `src/shared/api/generated`，不要手工修改。

## 项目检查

```powershell
cd eventhub-server
.\mvnw clean verify
```

```powershell
cd eventhub-web
npm run check
```

```powershell
cd eventhub-admin
npm run check
```

本地默认将 MySQL 映射到宿主机 `3307` 端口，以避开已有的本机 MySQL `3306` 服务。端口和开发凭据可通过根目录 `.env` 覆盖，真实 `.env` 不提交。

首次创建本地管理员时，在启动后端的 PowerShell 会话中设置：

```powershell
$env:BOOTSTRAP_ADMIN_USERNAME = 'admin'
$env:BOOTSTRAP_ADMIN_PASSWORD = '请替换为本地强密码'
$env:BOOTSTRAP_MERCHANT_USERNAME = 'merchant'
$env:BOOTSTRAP_MERCHANT_PASSWORD = '请替换为本地强密码'
$env:BOOTSTRAP_MERCHANT_NAME = '本地演示商家'
$env:AUTH_JWT_SECRET = '请替换为至少 32 位的随机字符串'
.\mvnw spring-boot:run
```

管理员和演示商家仅在账号不存在时创建，密码不会写入仓库或日志。

## 阶段 3 功能入口

- 用户端活动列表：http://localhost:3000/activities
- 用户端我的订单：http://localhost:3000/orders
- 管理端活动管理或审核：http://localhost:3001/activities
- 管理端订单查询：http://localhost:3001/orders
- 管理端场馆管理：http://localhost:3001/venues
- 管理端商家管理：http://localhost:3001/merchants

商家可以创建场馆、配置固定座位、创建活动和场次票档并提交审核。管理员审核通过后，用户可在活动详情选择场次，完成锁座、创建订单、模拟支付、取消和订单查询。

固定座位并发验收脚本：

```powershell
.\scripts\test-seat-lock-concurrency.ps1 `
  -AccessToken '<用户 Access Token>' `
  -SessionId 1 `
  -TicketTypeId 1 `
  -SessionSeatId 2 `
  -Requests 100
```

当前订单超时通过数据库定时扫描补偿，RabbitMQ 延迟消息与正式票券核销将在后续阶段接入。
