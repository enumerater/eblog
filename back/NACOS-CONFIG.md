# Nacos 配置中心 — 配置项说明

所有微服务的业务配置（数据库连接、Redis、JWT 密钥等）均存储在 Nacos 配置中心。
Nacos 地址: `http://127.0.0.1:8848/nacos` (账号: nacos / nacos)

以下为每个服务需要在 Nacos 中创建的配置（Properties 格式，DEFAULT_GROUP / public 命名空间）：

---

## 1. gateway-service

**Data ID:** `gateway-service`

```properties
jwt.public-key=your-rsa-public-key-base64

# 路由定义 (注意: filters 必须用点号展开, 不能用 inline 简写)
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=lb://auth-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/auth/**
spring.cloud.gateway.routes[0].filters[0].name=JwtAuth
spring.cloud.gateway.routes[0].filters[0].args.publicPaths=POST:/api/auth/login,POST:/api/auth/refresh

spring.cloud.gateway.routes[1].id=article-service
spring.cloud.gateway.routes[1].uri=lb://article-service
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/articles/**,/api/drafts/**,/api/upload/**
spring.cloud.gateway.routes[1].filters[0].name=JwtAuth
spring.cloud.gateway.routes[1].filters[0].args.publicPaths=GET:/api/articles,GET:/api/articles/**,GET:/api/upload/**

spring.cloud.gateway.routes[2].id=article-query-service
spring.cloud.gateway.routes[2].uri=lb://article-query-service
spring.cloud.gateway.routes[2].predicates[0]=Path=/api/articles-query/**
spring.cloud.gateway.routes[2].filters[0].name=JwtAuth
spring.cloud.gateway.routes[2].filters[0].args.publicPaths=GET:/api/articles-query/**

spring.cloud.gateway.routes[3].id=comment-service
spring.cloud.gateway.routes[3].uri=lb://comment-service
spring.cloud.gateway.routes[3].predicates[0]=Path=/api/comments/**
spring.cloud.gateway.routes[3].filters[0].name=JwtAuth
spring.cloud.gateway.routes[3].filters[0].args.publicPaths=GET:/api/comments/**,POST:/api/comments

spring.cloud.gateway.routes[4].id=search-service
spring.cloud.gateway.routes[4].uri=lb://search-service
spring.cloud.gateway.routes[4].predicates[0]=Path=/api/search/**
spring.cloud.gateway.routes[4].filters[0].name=JwtAuth
spring.cloud.gateway.routes[4].filters[0].args.publicPaths=GET:/api/search/**

spring.cloud.gateway.routes[5].id=intelligence-service
spring.cloud.gateway.routes[5].uri=lb://intelligence-service
spring.cloud.gateway.routes[5].predicates[0]=Path=/api/intelligence/**
spring.cloud.gateway.routes[5].filters[0].name=JwtAuth
spring.cloud.gateway.routes[5].filters[0].args.publicPaths=GET:/api/intelligence/**

spring.cloud.gateway.routes[6].id=notification-service
spring.cloud.gateway.routes[6].uri=lb://notification-service
spring.cloud.gateway.routes[6].predicates[0]=Path=/api/notifications/**
spring.cloud.gateway.routes[6].filters[0].name=JwtAuth
spring.cloud.gateway.routes[6].filters[0].args.publicPaths=

spring.cloud.gateway.routes[7].id=file-service
spring.cloud.gateway.routes[7].uri=lb://file-service
spring.cloud.gateway.routes[7].predicates[0]=Path=/api/files/**
spring.cloud.gateway.routes[7].filters[0].name=JwtAuth
spring.cloud.gateway.routes[7].filters[0].args.publicPaths=GET:/api/files/**

# Redis (用于 Token 黑名单)
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
spring.data.redis.timeout=3000
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
```

---

## 2. auth-service

**Data ID:** `auth-service`

```properties
# 数据库
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/my_blog?useUnicode=true&characterEncoding=utf8mb4&useSSL=false
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JWT 密钥 (RSA, Base64 编码)
# 首次启动时会自动生成并打印在日志中, 复制到此处
jwt.private-key=MIIEvQIBADANBgkqhkiG9w0BAQEFAASC...
jwt.public-key=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...

# Redis
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379

# MyBatis-Plus
mybatis-plus.mapper-locations=classpath:mapper/*.xml
mybatis-plus.type-aliases-package=com.enumerate.auth.entity

# GitHub OAuth (登录时使用)
# oauth.github.client-id=your-github-client-id
# oauth.github.client-secret=your-github-client-secret
```

---

## 3. article-service

**Data ID:** `article-service`

```properties
# 数据库
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/my_blog?useUnicode=true&characterEncoding=utf8mb4&useSSL=false
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# MyBatis-Plus
mybatis-plus.mapper-locations=classpath:mapper/*.xml

# 阿里云 OSS (可选, 不配置则上传功能不可用)
# aliyun.oss.endpoint=https://oss-cn-hangzhou.aliyuncs.com
# aliyun.oss.access-key-id=your-access-key
# aliyun.oss.access-key-secret=your-secret
# aliyun.oss.bucket-name=your-bucket
```

---

## 4. article-query-service

**Data ID:** `article-query-service`

```properties
# 数据库 (与 article-service 共享 my_blog)
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/my_blog?useUnicode=true&characterEncoding=utf8mb4&useSSL=false
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# MyBatis-Plus
mybatis-plus.mapper-locations=classpath:mapper/*.xml

# Redis (缓存 + 阅读量统计)
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
```

---

## 5. comment-service

**Data ID:** `comment-service`

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/my_blog?useUnicode=true&characterEncoding=utf8mb4&useSSL=false
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

mybatis-plus.mapper-locations=classpath:mapper/*.xml
```

---

## 6. search-service

**Data ID:** `search-service`

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/my_blog?useUnicode=true&characterEncoding=utf8mb4&useSSL=false
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

mybatis-plus.mapper-locations=classpath:mapper/*.xml

# Redis (热搜排行)
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
```

---

## 7. intelligence-service

**Data ID:** `intelligence-service`

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/my_blog?useUnicode=true&characterEncoding=utf8mb4&useSSL=false
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

mybatis-plus.mapper-locations=classpath:mapper/*.xml
```

---

## 8. notification-service

**Data ID:** `notification-service`

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/my_blog?useUnicode=true&characterEncoding=utf8mb4&useSSL=false
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

mybatis-plus.mapper-locations=classpath:mapper/*.xml
```

---

## 9. file-service

**Data ID:** `file-service`

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/my_blog?useUnicode=true&characterEncoding=utf8mb4&useSSL=false
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

mybatis-plus.mapper-locations=classpath:mapper/*.xml

# 文件存储: local (默认) 或 oss
# file.storage.type=local
# file.storage.local.path=./upload
# file.storage.local.max-size=10485760
```

---

## 快速配置脚本

首次启动 Nacos 后，可以在 Nacos 控制台逐个创建以上配置。
也可以通过 Nacos Open API 批量创建，示例:

```bash
# 以 article-query-service 为例
curl -X POST 'http://127.0.0.1:8848/nacos/v1/cs/configs' \
  -d 'dataId=article-query-service&group=DEFAULT_GROUP&content=spring.datasource.url=jdbc:mysql://127.0.0.1:3306/my_blog%3FuseUnicode=true%26characterEncoding=utf8mb4%26useSSL=false%0aspring.datasource.username=root%0aspring.datasource.password=1234%0amybatis-plus.mapper-locations=classpath:mapper/*.xml&type=properties'
```