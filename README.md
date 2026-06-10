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
