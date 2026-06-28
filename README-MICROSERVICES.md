# eblog 微服务 — 启动与开发指南

## 前置依赖

| 服务 | 用途 | 获取方式 |
|------|------|---------|
| **MySQL 8.x** | 持久化存储 | `docker compose up -d mysql` |
| **Redis 7.x** | 缓存 + Token存储 | `docker compose up -d redis` |
| **Nacos 2.4.x** | 注册中心 + 配置中心 | `docker compose up -d nacos` |
| **RocketMQ 5.x** | 异步消息 + 事件驱动 | `docker compose up -d rocketmq-*` |
| **Java 17+** | 运行环境 | 已安装 (23.0.2) |
| **Maven 3.9+** | 构建工具 | 已安装 (3.9.9) |

## 启动顺序

```
                  ┌─────────┐
                  │  MySQL  │
                  └────┬────┘
                       │
              ┌────────┼────────┐
              ▼        ▼        ▼
          ┌──────┐ ┌──────┐ ┌──────┐
          │ Redis│ │Nacos │ │Rocket│
          └──────┘ └──┬───┘ │MQ   │
                      │     └──────┘
              ┌───────┼───────┐
              ▼       ▼       ▼
          ┌──────┐ ┌──────┐ ┌──────┐
          │common│ │ Auth │ │Gateway│
          │ 模块  │ │Service│ │Service│
          └──────┘ └──────┘ └──────┘
```

## Docker 一键启动基础设施

```bash
# 进入项目根目录
cd eblog

# 启动所有基础设施 (MySQL, Redis, Nacos, RocketMQ, Canal)
docker compose up -d

# 仅启动部分服务 (开发时不需要所有服务)
docker compose up -d mysql redis nacos
```

## 初始化数据库

```bash
# 方式 1: 容器启动时自动初始化 (docker-compose 挂载了 sql/init/)
# 方式 2: 手动执行
mysql -u root -p1234 -h 127.0.0.1 < sql/init/init_auth.sql
mysql -u root -p1234 -h 127.0.0.1 < sql/init/init_article_write.sql
mysql -u root -p1234 -h 127.0.0.1 < sql/init/init_article_read.sql
mysql -u root -p1234 -h 127.0.0.1 < sql/init/init_comment.sql
```

## Nacos 配置初始化

启动 Nacos 后，在控制台 `http://localhost:8848/nacos` (账号: nacos/nacos) 创建以下配置：

| Data ID | Group | 说明 |
|---------|-------|------|
| `application-dev.yml` | `DEFAULT_GROUP` | 共享配置 (数据源, Redis) |
| `gateway-service-dev.yml` | `DEFAULT_GROUP` | Gateway 专属配置 |
| `auth-service-dev.yml` | `DEFAULT_GROUP` | Auth 专属配置 |
| `sentinel-gateway-rules.json` | `DEFAULT_GROUP` | Sentinel 限流规则 |

YAML 内容请参考 `config/nacos/` 目录下的文件。

## 启动微服务

### 方式 1: 使用 Nacos (推荐)

```bash
# 编译公共模块
cd common && mvn clean install -DskipTests

# 启动 Auth Service (端口 8081)
cd services/auth-service
mvn spring-boot:run

# 启动 Gateway (端口 8080)
cd services/gateway-service
mvn spring-boot:run
```

### 方式 2: 不使用 Nacos (本地开发)

```bash
# 启动 Auth Service
cd services/auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 启动 Gateway
cd services/gateway-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 验证服务启动

```bash
# 1. 注册中心确认 (Nacos 控制台)
open http://localhost:8848/nacos

# 2. Auth Service 健康检查
curl http://localhost:8081/actuator/health

# 3. Gateway 路由检查
curl http://localhost:8080/actuator/health

# 4. 测试登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"1234"}'
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `NACOS_ADDR` | `127.0.0.1:8848` | Nacos 地址 |
| `NACOS_NAMESPACE` | `dev` | Nacos 命名空间 |
| `MYSQL_HOST` | `127.0.0.1` | MySQL 地址 |
| `MYSQL_USER` | `root` | MySQL 用户 |
| `MYSQL_PASSWORD` | `1234` | MySQL 密码 |
| `REDIS_HOST` | `127.0.0.1` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `JWT_PUBLIC_KEY` | (自动生成) | RSA 公钥 (Base64) |
| `JWT_PRIVATE_KEY` | (自动生成) | RSA 私钥 (Base64) |
| `ADMIN_USERNAME` | `admin` | 管理员用户名 |
| `ADMIN_PASSWORD` | `1234` | 管理员密码 |

## 常见问题

### Q: Nacos 连接失败怎么办？
A: 使用 local profile 启动：`mvn spring-boot:run -Dspring-boot.run.profiles=local`

### Q: Auth Service 启动时没打印 RSA 密钥？
A: 密钥对在首次启动时自动生成并打印在日志中。将公钥 (`JWT_PUBLIC_KEY`) 配置到 Gateway 的 `jwt.public-key` 中。

### Q: 如何重新生成 RSA 密钥？
A: 删除配置中的 `jwt.private-key` 和 `jwt.public-key`，重启 Auth Service 即可。
