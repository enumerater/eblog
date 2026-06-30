# eblog — 微服务博客系统

> **项目定位**：基于 Spring Cloud Alibaba 生态的微服务博客平台，覆盖文章管理、全文搜索、内容推荐、评论互动等完整博客功能链路。
>
> **技术栈**：Java 17 · Spring Boot 3.4 · Spring Cloud 2024 · Spring Cloud Alibaba · Nacos · Sentinel · MyBatis-Plus · Redis · Caffeine · MySQL · RocketMQ · JJWT · GitHub OAuth
>
> **基础设施**：Docker Compose 编排（MySQL 8 + Redis 7 + Nacos 2.4 + RocketMQ 5.3 + Canal 1.1）

---

## 一、架构设计

### 1.1 微服务划分

| 服务 | 端口 | 核心职责 |
|------|------|---------|
| `gateway-service` | 8080 | 统一网关（路由 / JWT 认证 / Sentinel 限流 / 统一降级 / CORS） |
| `auth-service` | 8081 | 认证授权（管理员登录 / GitHub OAuth / 双 Token 签发与状态管理） |
| `article-service` | 8082 | 文章写服务（CRUD / 草稿管理 / 内容发布 / **MQ 事件生产者**） |
| `article-query-service` | 8083 | 文章读服务（分页 / 详情 / 标签聚合 / **Caffeine+Redis 二级缓存** / 阅读计数） |
| `comment-service` | 8084 | 评论系统（嵌套回复 / **MQ 事件生产者** / 删除权限控制） |
| `search-service` | 8085 | 全文搜索（关键词搜索 / 搜索联想 / 热搜实时排行） |
| `intelligence-service` | 8086 | 智能分析（摘要 / 关键词 / 阅读时长 / **MQ 异步消费** / 标签建议 / 推荐） |
| `notification-service` | 8087 | 通知系统（站内通知 / 广播消息 / **MQ 异步消费** / 已读管理） |
| `file-service` | 8088 | 文件管理（上传 / 按日期归档 / MD5 去重 / 可插拔存储后端） |

### 1.2 流量路径

```
所有客户端请求 → Gateway (单一入口)
    ├─ 公开路径（GET 文章列表/详情、搜索）→ 直接放行
    ├─ 认证路径（登录/OAuth）→ 直接放行 → auth-service
    └─ 受保护路径 → Gateway 本地 JWT 验签（RSA 公钥 + 黑名单检查）
                    ├─ 通过 → 注入用户身份 Header → 转发至业务服务
                    └─ 失败 → 统一 401 JSON 响应
```

### 1.3 CQRS —— 读写分离 + MQ 最终一致性

`article-service`（写）与 `article-query-service`（读）物理分离。写服务产生数据变更后通过 **RocketMQ 广播** `article-events` 事件，下游服务异步消费：

```
article-service (写)
    │ save/update/delete
    ▼
RocketMQ Topic: article-events
    ├─→ intelligence-service (异步生成摘要、关键词、统计)
    └─→ query-service (失效二级缓存 ← 保证读侧数据最终一致)
```

---

## 二、异步消息体系（RocketMQ）

RocketMQ 的引入使核心业务链路从同步耦合变为异步解耦。所有 MQ 发送失败均不会阻塞主流程，通过日志记录和 RocketMQ 内置重试（默认 16 次）保证最终可靠。

### 2.1 Topic 定义

| Topic | 生产者 | 消费者 | 消息体 |
|-------|--------|--------|--------|
| `article-events` | article-service | intelligence-service, query-service | `ArticleEventDTO` |
| `comment-events` | comment-service | notification-service | `CommentEventDTO` |

### 2.2 场景 A：文章发布/更新 → 异步智能分析

```
用户发布文章
    → article-service 写入 MySQL
    → publish ArticleEventDTO → RocketMQ (异步)
    → intelligence-service 消费
        ├─ ARTICLE_CREATED → 生成摘要、关键词、字数、阅读时长
        ├─ ARTICLE_UPDATED → 重新生成分析
        └─ ARTICLE_DELETED → 清理分析记录
```

用户写文章时无需等待分析完成，接口秒级返回。分析结果写入 `article_analysis` 表，后续查询直接读取。

### 2.3 场景 B：评论创建 → 异步通知

```
用户发表评论
    → comment-service 写入 MySQL
    → publish CommentEventDTO → RocketMQ (异步)
    → notification-service 消费
        └─ COMMENT_CREATED → 创建站内通知 √
```

评论写入与通知发送解耦，即使 notification-service 临时不可用，RocketMQ 会在其恢复后重新投递消息。

### 2.4 场景 C：文章变更 → 缓存失效广播

```
文章被更新/删除
    → article-service publish → RocketMQ
    → query-service 消费
        ├─ evict cache:article:detail:{id}  (详情缓存)
        ├─ evict cache:article:tags          (标签聚合缓存)
        └─ 分页缓存保留 TTL 自动过期        (避免全量扫描 Key)
```

当 article-service 无法感知有哪些下游服务缓存了它的数据时，通过 MQ 广播一次性通知所有相关服务，保证缓存最终一致。

---

## 三、多级缓存体系

### 3.1 架构

```
读取路径:  请求 → L1 (Caffeine 堆内) → L2 (Redis) → DB (cache-aside)
写入/失效: 写操作 → 同时写入 L1 + L2 → MQ 广播通知其他服务失效缓存
降级策略:  Redis 不可用 → 降级为 Caffeine-only → 仍能提供低延迟响应
```

### 3.2 L1 Caffeine + L2 Redis

`MultiCacheManager` 组件封装两层缓存：

- L1（Caffeine）：堆内缓存，极低延迟（纳秒级），默认 5000 条，30 分钟过期
- L2（Redis）：分布式共享，默认 60 分钟 TTL
- 内置统计 API：实时输出**命中率、命中/未命中计数**
- Redis 异常时自动降级，不影响 Caffeine 正常使用

### 3.3 缓存策略矩阵

| 数据 | Key 模式 | TTL | 失效方式 | 说明 |
|------|---------|-----|---------|------|
| 文章详情 | `cache:article:detail:{id}` | 30 min | MQ evict + TTL | 高频读低频写 |
| 文章分页 | `cache:article:page:{p}:{s}:{tag}` | 5 min | TTL 自动过期 | 短 TTL 保证新鲜度 |
| 标签聚合 | `cache:article:tags` | 30 min | MQ evict + TTL | 全量聚合计算成本高 |
| 搜索结果 | 使用 Redis ZSet | - | 实时更新 | 热搜榜本身已是缓存 |

---

## 四、核心实现细节

### 4.1 网关层 JWT 认证 —— RSA 本地验签

大多数微服务博客在 Gateway 做认证时，会将 Token 透传给 Auth Service 验证，引入一次额外的 RPC 调用。

**本项目方案**：

- Auth Service 持有 RSA **私钥**（签名），Gateway 持有 RSA **公钥**（验签），私钥**从不经过网络传输**
- Gateway 的 `JwtAuthGatewayFilterFactory` 在网关层**直接**调用 `Jwts.parser().verifyWith(publicKey)` 完成签名验证
- 验签通过后，通过 `ReactiveRedisTemplate` 检查 Token 是否在黑名单（登出/吊销），整个流程**纯响应式、零阻塞**
- 公钥未加载时拒绝请求，Redis 不可用时跳过黑名单检查，仅凭签名验证放行 —— 实现优雅降级

**双 Token + 滑动窗口**：

| Token 类型 | 有效期 | 存储位置 | 职责 |
|-----------|--------|---------|------|
| Access Token | 15 分钟 | 客户端（无服务端状态） | 请求认证，Gateway 本地验签 |
| Refresh Token | 7 天 | 客户端 + Redis | 刷新 Access，Redis 存在即有效 |

- 刷新时重置 Redis TTL（7 天滑动窗口，持续活跃的用户无需重新登录）
- 安全兜底：检测到旧 Refresh Token 被重放 → 删除 Redis 记录，强制重新登录

### 4.2 Redis 使用矩阵

| 用途 | Key 结构 | Redis 类型 | 所属服务 |
|------|---------|-----------|---------|
| Refresh Token 存储 | `auth:refresh:{userId}` | String | auth-service |
| Token 吊销黑名单 | `auth:blacklist:{jti}` | String | gateway |
| 文章阅读计数 | `article:view:{id}` | String | query-service |
| 热门文章排行 | `article:hot` | ZSet | query-service |
| 热搜关键词排行 | `search:hot` | ZSet | search-service |
| 文章详情/标签等 | `cache:article:*` | String | query-service (L2) |

### 4.3 Sentinel 分组限流 —— 按 API 语义分级治理

Gateway 层通过 Sentinel 对 API 按语义分组，配置差异化限流策略：

| API 分组 | 限流阈值 | 控制行为 | 设计意图 |
|----------|---------|---------|---------|
| 读接口群组 | 1000 QPS, Burst 200 | 快速失败 | 高吞吐，匹配博客读多写少场景 |
| 写接口群组 | 50 QPS, Burst 10 | 排队等待 | 保护写入一致性 |
| 认证接口群组 | 20 QPS | 快速失败 | 防暴力破解 |
| 热点文章 | 200 QPS/篇 | 参数限流 | 按 `?id=xxx` 单文章限流，防爆文打垮服务 |

`FallbackHandler`（`ErrorWebExceptionHandler`）统一捕获 5 种异常类型，返回标准降级 JSON。

### 4.4 全文搜索 + 实时热搜

```
请求 → search-service
  ├─ LIKE 查询（title + content）→ 返回结果
  ├─ 关键词高亮（<mark> 标签标红命中词）
  ├─ 记录到 search_logs 表
  └─ Redis ZSet INCR score（热搜热度实时递增）
```

- 搜索联想：`title LIKE 'prefix%'` 前缀匹配
- 热搜榜：`Redis ZREVRANGE search:hot 0 N` 毫秒级返回 Top-N

### 4.5 智能分析引擎

Intelligence Service 提供纯服务端实现的轻量内容分析，通过 MQ 异步触发：

- **关键词提取**：中文 n-gram（2~4 字）+ 停用词过滤 + 词频统计，取 Top 10
- **文章摘要**：HTML 标签清洗后截取前 200 字
- **阅读时长估算**：`max(1, 总字数 / 500)`
- **标签推荐**：Jaccard 交集匹配已有标签库
- **文章推荐**：计算目标文章标签与其他文章的 **Jaccard 相似度**（`|A∩B| / |A∪B|`）

### 4.6 嵌套评论系统 —— 两层展平

- 数据库以 `parent_id` 存储完整父子关系
- 查询时**展平为两层**：顶级评论 + 平铺回复列表
- 回复三级以上时，自动追溯直接父评论的 `author`，展示为 `"XXX 回复 @YYY"`
- 删除权限：仅作者和管理员可删

### 4.7 GitHub OAuth 集成

- 服务端向 GitHub 交换 access_token + 获取用户信息
- 支持代理配置（适配受限网络环境）
- 首次登录自动注册，后续更新用户信息 + 签发 JWT

### 4.8 可插拔文件存储

- `FileStorageService` 接口定义，策略模式解耦
- 默认 `LocalFileStorageService`：按日期目录归档、UUID 重命名
- 扩展方式：实现接口 + 修改配置即可切换

### 4.9 全局统一响应与异常处理

所有服务统一 `Result<T>` 响应格式。`ResultCode` 枚举定义完整的错误码体系（30+ 状态码），覆盖认证/参数/业务/服务/分布式事务五个层级。

全局异常处理器统一拦截 `BizException`、`ConstraintViolationException`、`MissingServletRequestParameterException`、`MethodArgumentTypeMismatchException` 和兜底异常。

### 4.10 Feign + Sentinel 熔断降级

`AuthClient` 通过 `FallbackFactory` 在 Auth Service 不可用时返回标准化降级错误，避免调用链阻塞。

---

## 五、基础设施

### 5.1 Docker Compose 编排

```yaml
mysql:8.0          # 数据库, utf8mb4, binlog ROW 模式
redis:7-alpine     # 缓存/状态/排行榜, AOF 持久化, 256MB LRU 淘汰
nacos:2.4.3        # 注册中心 + 配置中心
rocketmq:5.3.1     # 消息队列（Namesrv + Broker + Dashboard）
canal:1.1.7        # MySQL binlog 监听 → RocketMQ（预留 CDC 能力）
```

RocketMQ 控制台：`http://localhost:18080`

### 5.2 配置热更新

所有服务通过 Nacos 动态拉取配置，配合 Actuator `refresh` 端点实现配置热更新，无需重启服务。

### 5.3 数据库初始化

版本化的迁移脚本放置在 `sql/migration/` 目录，docker-compose 启动时自动初始化。

---

## 六、架构决策记录

| 决策 | 选项 | 选择理由 |
|------|------|---------|
| Gateway 层验签 | RSA 公钥本地验签 vs 透传 Auth 服务 | 本地验签 ≈ 0.1ms，RPC ≈ 5-10ms，50x 差距；私钥不经过网络 |
| 注册/配置中心 | Nacos vs Eureka + Config | 二合一减少运维组件；支持动态刷新；命名空间隔离 |
| 双 Token 策略 | Access + Refresh 滑动窗口 vs 单长过期 Token | 短时效 Access 降低泄露风险；滑动窗口提升体验 |
| 缓存架构 | Caffeine L1 + Redis L2 vs 仅 Redis | L1 纳秒级命中，无网络开销；L2 降级不阻断业务 |
| MQ 选型 | RocketMQ vs RabbitMQ / Kafka | 已有 Spring Cloud Alibaba 生态集成；支持事务消息（预留）；与 Canal 原生集成 |
| 消息解耦 | 异步 MQ vs 同步 Feign 调用 | 评论/分析等场景不需要实时响应；MQ 缓冲削峰；生产故障不传播 |
| 智能分析同步策略 | MQ 异步触发 vs 请求时同步计算 | 发布接口秒级返回；分析结果缓存复用；MQ 重试保证最终生成 |
| CQRS 同步 | MQ 事件广播 vs 直接共享 DB | 写服务不感知读服务存在；新增订阅方无需修改生产者 |
| 推荐算法 | Jaccard 相似度 vs 协同过滤/外部 API | 零外部依赖；标签稀疏场景比协同过滤更稳定 |

---

## 七、关于作者

本项目由我独立完成架构设计、编码实现与基础设施搭建。与技术博客常见的"框架用法罗列"不同，这个项目展示的是**真实的工程决策**：

- **知道什么场景选什么方案**：CQRS、RSA 本地验签、Jaccard 推荐、L1+L2 多级缓存、MQ 异步解耦
- **知道方案之间怎么取舍**：性能 vs 复杂度、同步 vs 异步、读延迟 vs 写延迟
- **知道异常情况怎么兜底**：Redis 不可用跳 Caffeine-only、Auth 不可用 Feign 降级、MQ 发送失败不阻塞主流程、Sentinel 熔断统一响应
- **知道为未来留什么扩展点**：`FileStorageService` 接口、Canal CDC 基础设施、通用 `ArticleEventDTO`/`CommentEventDTO` 事件模型

每一行代码都考虑了"如果出了问题怎么办"，而不是只写"正常情况怎么走"。

---

*eBlog — 每个设计决策都有数据支撑，每处异常都有自己的兜底。*