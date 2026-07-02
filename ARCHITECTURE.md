# eblog 微服务架构设计文档

> **版本**: v2.0（微服务重构方案，已实现版本）
>
> **作者**: enumerate
>
> **日期**: 2026-07-01

---

## 目录

1. [架构概览](#1-架构概览)
2. [服务划分与职责](#2-服务划分与职责)
3. [核心技术栈](#3-核心技术栈)
4. [架构实现详解](#4-架构实现详解)
   - [4.1 双 Token 与分布式会话](#41-双-token-与分布式会话)
   - [4.2 基于 Sentinel 的全链路韧性](#42-基于-sentinel-的全链路韧性)
   - [4.3 多级缓存防御体系](#43-多级缓存防御体系)
   - [4.4 分布式事务（Seata AT）](#44-分布式事务seata-at)
   - [4.5 事件驱动异步通信](#45-事件驱动异步通信)
   - [4.6 Elasticsearch 全文搜索](#46-elasticsearch-全文搜索)
   - [4.7 基于 CDC 的缓存失效](#47-基于-cdc-的缓存失效)
5. [数据架构](#5-数据架构)
6. [部署方案](#6-部署方案)
7. [目录结构](#7-目录结构)

---

## 1. 架构概览

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          客户端 (Browser / Mobile)                        │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │ HTTPS
                              ▼
┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
│  ╔═══════════════════════════════════════════════════════════════════╗  │
│  ║           Spring Cloud Gateway (统一入口层)                        ║  │
│  ║  路由转发 · JWT认证(Dual Token) · Sentinel流控 · 请求响应日志     ║  │
│  ╚═══════════════════════════════════════════════════════════════════╝  │
│ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │
│  ╔═══════════════════════════════════════════════════════════════════╗  │
│  ║              Nacos（注册中心 + 配置中心）                           ║  │
│  ╚═══════════════════════════════════════════════════════════════════╝  │
└ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
                              │
    ┌────────┬────────┬──────┼────────┬────────┬────────┐
    ▼        ▼        ▼      ▼        ▼        ▼        ▼
┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐
│ Auth  │ │Article│ │Query  │ │Search │ │Comment│ │Notif  │ │File   │
│Service│ │Service│ │Service│ │Service│ │Service│ │Service│ │Service│
│       │ │       │ │       │ │(ES)   │ │       │ │       │ │       │
│ 认证  │ │ 文章  │ │ 列表  │ │ 全文  │ │ 评论  │ │ 通知  │ │ 文件  │
│ 授权  │ │ CRUD  │ │ 详情  │ │ 检索  │ │ 管理  │ │ 推送  │ │ 存储  │
│ OAuth  │ │ 事件  │ │ 缓存  │ │ 高亮  │ │ 审批  │ │ 广播  │ │ 去重  │
└───┬───┘ └───┬───┘ └───┬───┘ └───┬───┘ └───┬───┘ └───┬───┘ └───┬───┘
    │         │         │         │         │         │         │
    └─────────┼─────────┼─────────┼─────────┼─────────┼─────────┘
              │         │         │         │
               ▼        ▼         ▼         ▼
        ┌─────────────────────────────────────────────┐
        │           RocketMQ（异步解耦 + 事件驱动）      │
        │   Article Events · Comment Events · Search  │
        └─────────────────────┬───────────────────────┘
                              │
        ┌─────────────────────▼───────────────────────┐
        │         Canal (MySQL Binlog 监听)             │
        │    监听文章表 binlog → 推送缓存失效事件         │
        └─────────────────────────────────────────────┘

        ┌──────────────────────────────────────────────┐
        │              Seata (分布式事务)                │
        │   AT 模式: 跨服务写入（文章 + 评论 + 通知）    │
        └──────────────────────────────────────────────┘
```

### 1.2 请求链路示例：用户浏览文章

```
Browser → Gateway (验证Token, 限流)
        → 查询 Redis 缓存 → 命中 → 返回
        → 未命中 → ArticleQueryService → MySQL → 返回
        → 异步写入 Redis
```

### 1.3 请求链路示例：用户发布文章

```
Browser → Gateway → ArticleService (Seata AT 全局事务)
        → 写入 MySQL
        → 发送 RocketMQ 事件
        → Commit 全局事务
        → 返回成功
        └→ (异步) CommentService/NotificationService 消费 MQ 事件
```

---

## 2. 服务划分与职责

### 2.1 服务粒度

| 服务 | 职责 | 关键特性 |
|------|------|---------|
| **Auth Service** | 认证与授权 | 双Token、OAuth2.0(GitHub)、RSA非对称签名 |
| **Article Service** | 文章写模型 | CRUD、Seata分布式事务、事件发布 |
| **Article Query Service** | 文章读模型 | 高性能查询、多级缓存(Redis)、布隆过滤器 |
| **Search Service** | 全文检索 | ES + IK分词、高亮、搜索建议、热搜榜 |
| **Comment Service** | 评论管理 | 嵌套评论、审核、Seata分布式事务 |
| **Notification Service** | 通知管理 | 通知CRUD、RocketMQ消费、管理员广播 |
| **File Service** | 文件存储 | 上传下载、MD5去重、本地文件存储 |
| **Gateway Service** | API网关 | 路由、JWT认证、Sentinel限流、降级 |

### 2.2 服务职责详述

#### Auth Service（认证服务）

```
核心职责:
  - 管理员登录/登出
  - 双Token签发与验证（Access Token + Refresh Token）
  - Token自动续期（滑动窗口策略）
  - OAuth2.0 社交登录（GitHub）
  - Token吊销（Redis黑名单）

技术实现:
  - RSA256非对称签名：AuthService持有私钥、Gateway持有公钥
  - Access Token: 15分钟有效期，携带 userId/role/nickname
  - Refresh Token: 7天有效期，Redis持久化，支持主动吊销
  - 滑动窗口刷新：每次刷新续期Refresh Token的TTL
  - Token重用检测：Redis中标记已使用Token，防止泄露
```

#### Article Service（文章服务 — 写模型）

```
核心职责:
  - 文章的创建、编辑、删除
  - 发布文章时触发RocketMQ事件
  - 文章图片上传（阿里云OSS集成）
  - 草稿管理（保存/发布/编辑）

技术实现:
  - Seata AT `@GlobalTransactional` 保证文章写入一致性
  - RocketMQ 异步事件通知下游服务（查询缓存失效、评论通知）
  - OSS 图片上传支持
  - MyBatis-Plus ORM
```

#### Article Query Service（文章查询服务 — 读模型）

```
核心职责:
  - 文章列表查询（分页 + 标签过滤）
  - 文章详情查询
  - 热点文章排行
  - 标签云聚合统计

技术实现:
  - 布隆过滤器 (BloomFilter) — 缓存穿透防护
  - Redis 缓存 — 热点文章分布式缓存
  - Galera / CDC 事件驱动缓存失效
```

#### Search Service（搜索服务）

```
核心职责:
  - Elasticsearch 全文检索
  - 文章索引建立与全量重建
  - 搜索建议（前缀匹配）
  - 搜索词高亮展示
  - 热搜榜单（Redis ZSet）

技术实现:
  - IK 中文分词器（ik_smart + ik_max_word）
  - multi_match 查询（title^3, tags^2, content）
  - 高亮（<mark> 标签包裹命中词）
  - Redis ZSet 热搜排行
  - MySQL 搜索日志记录
```

#### Comment Service（评论服务）

```
核心职责:
  - 评论CRUD
  - 评论审核（审批通过/拒绝）
  - 楼中楼回复（两级嵌套）
  - 评论计数

技术实现:
  - 邻接表模型（parentId 自关联）
  - Java 内存遍历组装二级嵌套
  - Seata AT 全局事务保证评论写入一致性
  - Feign 同步 + RocketMQ 异步双路通知 NotificationService
```

#### File Service（文件服务）

```
核心职责:
  - 文件上传/下载/删除
  - MD5 内容去重（同文件不重复存储）
  - 分页文件列表管理

技术实现:
  - 本地文件系统存储
  - 策略模式（FileStorageService接口），可扩展OSS/MinIO
  - 按月/日目录归档存储
```

#### Notification Service（通知服务）

```
核心职责:
  - 通知CRUD（分页查询）
  - 评论回复通知（MQ消费 + Feign 同步创建）
  - 管理员广播通知
  - 未读计数

技术实现:
  - MySQL 持久化通知存储（isRead 标志）
  - RocketMQ 消费 CommentEvent 自动创建评论通知
  - 前端轮询获取未读计数
```

#### Gateway Service（API网关）

```
核心职责:
  - 路由转发（按服务名路由到下游微服务）
  - 统一认证（JWT Token 验证 — RSA公钥本地验签）
  - 统一限流（Sentinel 网关流控）
  - 日志记录（TraceId 透传）

技术实现:
  - JwtAuthGatewayFilterFactory：自定义 GatewayFilter，RSA256 验签
  - 白名单配置：公开接口跳过认证
  - 用户信息透传：验签后 userId/role 注入请求头
  - Sentinel API分组限流：读接口1000 QPS、写接口50 QPS、认证20 QPS
  - Sentinel 热点参数限流：单篇文章200 QPS
  - Sentinel 自适应熔断：慢调用比例 + 异常比例
  - 统一降级处理：FallbackHandler 返回 Result 格式
```

---

## 3. 核心技术栈

### 3.1 技术选型总表

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **服务框架** | Spring Boot | 3.5.x | 服务基础框架 |
| **微服务** | Spring Cloud Alibaba | 2023.x | 微服务解决方案 |
| **注册/配置** | Nacos | 2.4.x | 注册中心 + 配置中心 |
| **远程调用** | OpenFeign | 4.x | 声明式HTTP调用 |
| **网关** | Spring Cloud Gateway | 4.x | API网关（WebFlux） |
| **限流熔断** | Sentinel | 1.8.x | 流量控制 + 熔断降级 |
| **分布式事务** | Seata | 2.x | AT模式分布式事务 |
| **消息队列** | RocketMQ | 5.x | 异步解耦 + 事件驱动 |
| **CDC** | Canal | 1.1.x | MySQL Binlog监听 |
| **搜索引擎** | Elasticsearch | 8.x | 全文检索（IK分词器） |
| **缓存 L1** | Caffeine | 3.x | 本地堆内缓存 |
| **缓存 L2** | Redis | 7.x | 分布式缓存 |
| **数据库** | MySQL | 8.x | 持久化存储 |
| **ORM** | MyBatis-Plus | 3.5.x | 数据库ORM |
| **网关认证** | JWT (RSA256) | 0.12.x | 无状态认证 |
| **容器化** | Docker Compose | - | 基础设施一键部署 |
| **认证** | JJWT + RSA | 0.12.x | 双Token签发与验签 |

---

## 4. 架构实现详解

---

### 4.1 双 Token 与分布式会话

> **难度**: ⭐⭐⭐

#### 4.1.1 双Token机制

```
┌──────────────────────────────────────────────────────────────────┐
│                         双Token策略                              │
│                                                                  │
│  Access Token (短时效)          Refresh Token (长时效)           │
│  ├─ 存储位置: 浏览器内存        ├─ 存储位置: httpOnly Cookie    │
│  ├─ 有效期: 15分钟             ├─ 有效期: 7天                  │
│  ├─ 用途: 每次请求的认证凭证     ├─ 用途: 无感刷新Access Token  │
│  ├─ 签名算法: RS256 (非对称)    ├─ 签名算法: RS256              │
│  └─ 含Payload: userId, role     └─ 服务端Redis存储: 支持吊销    │
│                                                                  │
│  ┌─────────────┐         ┌─────────────┐                        │
│  │ Gateway     │         │ Auth        │                        │
│  │ 验证Access  │         │ Service     │                        │
│  │ Token       │ ←────── │ Refresh &   │                        │
│  │ 过期 → 302  │         │ 签发新的    │                        │
│  │ Redirect    │         │ Access      │                        │
│  └─────────────┘         └─────────────┘                        │
└──────────────────────────────────────────────────────────────────┘
```

#### 4.1.2 Gateway统一验证

Gateway 使用 **RSA 公钥** 本地验证 Access Token（无需调用 Auth Service），验证通过后在请求头添加用户信息转发到下游服务。

#### 4.1.3 Token吊销策略

| 场景 | 处理方式 |
|------|---------|
| 修改密码 | 删除Redis中该用户的所有Refresh Token |
| 用户登出 | 删除特定设备的Refresh Token |
| Token重用检测 | Redis检查JTI是否已被消耗，防止泄露利用 |

---

### 4.2 基于 Sentinel 的全链路韧性

> **难度**: ⭐⭐⭐⭐

#### 4.2.1 Sentinel 应用层级

```
Gateway 层:
  ├─ 路由粒度限流: /api/articles/** → 1000 QPS
  ├─ API分组限流: 读接口 1000 QPS, 写接口 50 QPS
  ├─ 热点参数限流: ?id=hot-article → 单个文章200 QPS
  └─ 自适应熔断: 慢调用/异常比例熔断

服务 层:
  ├─ Feign调用隔离: 每个下游服务独立线程池
  │   舱壁大小: 核心10, 最大20, 队列10
  └─ 熔断降级: 慢调用比例 + 异常比例熔断
```

#### 4.2.2 降级策略

| 降级场景 | 行为 |
|---------|------|
| 文章服务熔断 | 降级JSON + 记录审计日志 |
| 搜索服务熔断 | 降级为MySQL LIKE搜索（评论区浏览不受影响） |
| 评论服务熔断 | 暂不展示评论区域 |

---

### 4.3 多级缓存防御体系

> **难度**: ⭐⭐⭐⭐

#### 4.3.1 架构层级

```
请求到达 Article Query Service
     │
     ▼
┌──────────────────────────────────────────────┐
│  Level 1: Bloom Filter                       │
│  判断请求的Key是否存在                        │
│  不存在 → 直接返回空 (防止缓存穿透)          │
│  存在 → 进入L2                               │
└──────────────────┬───────────────────────────┘
                   ▼
┌──────────────────────────────────────────────┐
│  Level 2: Caffeine (本地堆内缓存)            │
│  热点数据直接服务，0ms网络开销               │
│  配置：最大条目5000, 过期时间5分钟           │
│  未命中 → 查L3                               │
└──────────────────┬───────────────────────────┘
                   ▼
┌──────────────────────────────────────────────┐
│  Level 3: Redis (分布式缓存)                 │
│  多服务共享缓存，互相补充                    │
│  未命中 → 查数据库 + 写入缓存                │
└──────────────────┬───────────────────────────┘
                   ▼
┌──────────────────────────────────────────────┐
│  Level 4: 数据库 (兜底查询)                  │
│  查询 + 缓存回写                              │
└──────────────────────────────────────────────┘
```

#### 4.3.2 防护策略

| 问题 | 策略 | 实现 |
|------|------|------|
| **缓存穿透** | 布隆过滤器 | 启动时加载所有文章ID到布隆过滤器，拦截不存在ID的查询 |
| **缓存雪崩** | 过期时间加随机值 | `expire = base + random(0, 300)` 秒 |
| **缓存击穿** | 互斥锁 + 双重检测 | 热点key过期时，只允许一个线程查DB |

---

### 4.4 分布式事务（Seata AT）

> **难度**: ⭐⭐⭐⭐

#### 4.4.1 应用场景

博客系统中的跨服务写入场景：

```
发布文章 = ArticleService写入文章 + CommentService写入评论 + NotificationService创建通知
```

#### 4.4.2 Seata AT模式

使用 `@GlobalTransactional` 注解，Seata 自动拦截 JDBC 数据源，通过**两阶段提交**保证跨服务一致性：

```
┌─── 执行阶段 ──────────────────────────────────┐
│  1. ArticleService: INSERT article             │
│  2. CommentService: INSERT comment             │
│  3. NotificationService: INSERT notification   │
│  (Seata 自动记录 undo_log)                     │
├────────────────────────────────────────────────┤
│  所有分支成功 → 全局提交                       │
│  任一分支失败 → 全局回滚 (自动执行 undo)       │
└────────────────────────────────────────────────┘
```

使用 Seata AT 的利益：**零业务入侵**，只加一个注解就实现跨服务分布式事务，无需编写补偿代码。

---

### 4.5 事件驱动异步通信

> **难度**: ⭐⭐⭐⭐

#### 4.5.1 消息流总图

```
文章创建/更新/删除   →  ArticleService → RocketMQ (article-events topic)
                                         ├─ ArticleQueryService: 缓存失效
                                         └─ IntelligenceService: (预留)
评论创建/删除       →  CommentService → RocketMQ (comment-events topic)
                                         └─ NotificationService: 创建通知
```

#### 4.5.2 架构设计

```
应用写入 MySQL (Seata 全局事务)
    │
    ▼
┌──────────────┐
│  Producer    │ RocketMQTemplate.convertAndSend()
│  异步发送     │ 异常时 try-catch 记录日志 (非阻塞)
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  MQ Topic    │ 消息体: { eventType, articleId, timestamp }
└──────┬───────┘
       │
       ▼ (消费)
┌──────────────────────────────────┐
│  Consumer                        │
│  1. 反序列化事件                  │
│  2. 执行业务逻辑 (缓存失效/创建通知) │
│  3. ACK MQ (异常时重试)           │
└──────────────────────────────────┘
```

---

### 4.6 Elasticsearch 全文搜索

> **难度**: ⭐⭐⭐

#### 4.6.1 搜索架构

```
搜索请求 → SearchService → ES 全文检索 → 高亮结果 → 返回
                              │
                          IK分词器
                    title: ik_smart (粗粒度)
                    content: ik_max_word (细粒度)
```

#### 4.6.2 搜索策略

| 策略 | 说明 |
|------|------|
| **multi_match** | 关键词在 title^3 / tags^2 / content 多字段联合搜索，权重排序 |
| **IK 分词** | ik_smart（标题/摘要）+ ik_max_word（内容），中文语义分词 |
| **高亮** | 命中词使用 `<mark>` 标签包裹，前端渲染黄色高亮 |
| **搜索建议** | title前缀匹配（ES Prefix Query） |
| **热搜榜** | Redis ZSet 记录搜索频次 |
| **降级** | ES不可用时降级到MySQL LIKE搜索 |

---

### 4.7 基于 CDC 的缓存失效

> **难度**: ⭐⭐⭐⭐

#### 4.7.1 架构方案

```
应用写 MySQL (事务内)
    │
    ▼ (binlog)
┌──────────────┐
│  Canal       │ 伪装成MySQL Slave，实时拉取binlog
│  Server      │ 解析binlog为row-based JSON
└──────┬───────┘
       │ RocketMQ
       ▼
┌──────────────┐
│  MQ Topic    │ 消息体: { table, eventType, data }
│  cdc-topic   │ 消费者: QueryService → 缓存失效
└──────┬───────┘
       │
       ▼
┌──────────────────────────────┐
│  ArticleCacheEvictConsumer   │
│  Canal binlog → 缓存失效     │
│  保证缓存与数据库最终一致性    │
└──────────────────────────────┘
```

#### 4.7.2 数据一致性保障

| 问题 | 解决方案 |
|------|---------|
| 数据丢失 | Canal 持久化 binlog offset，重启后可断点续传 |
| 缓存不一致 | Canal 实时监听 binlog，秒级触发缓存失效 |

---

## 5. 数据架构

### 5.1 数据库（MySQL）

```
MySQL 实例
├─ my_blog                     # 统一数据库
│   ├─ user                    # 管理员用户 + OAuth账号
│   ├─ article                 # 文章主表
│   ├─ draft                   # 草稿表
│   ├─ comment                 # 评论表 (parentId 自关联)
│   ├─ search_log              # 搜索日志
│   ├─ article_analysis        # 文章分析记录
│   ├─ notification            # 通知表
│   ├─ file_record             # 文件记录
│   └─ recommendation_log      # 推荐记录
```

### 5.2 缓存（Redis）

```
Redis
├─ cache:article:{id}              # 文章缓存
├─ search:hot                      # 热搜榜 (ZSet)
├─ refresh_token:{userId}          # Refresh Token存储
└─ bloom:article_ids               # 布隆过滤器
```

### 5.3 搜索引擎（Elasticsearch）

```
Elasticsearch
├─ index: articles
│   ├─ title:         text (ik_smart)
│   ├─ content:       text (ik_max_word)
│   ├─ tags:          keyword
│   ├─ summary:       text
│   └─ created_at:    date
```

---

## 6. 部署方案

### 6.1 开发环境 (Docker Compose)

```yaml
# 基础设施 Docker Compose
services:
  mysql:         # MySQL 8.x
  redis:         # Redis 7.x
  nacos:         # Nacos 2.4.x (standalone)
  rocketmq:      # RocketMQ 5.x (namesrv + broker + dashboard)
  seata-server:  # Seata Server 2.x
  elasticsearch: # ES 8.x + IK 分词器
  kibana:        # Kibana (仅开发环境)
  canal:         # Canal 1.1.x (Binlog → RocketMQ)
```

业务服务（Gateway、Auth、Article、Query、Search、Comment、Notification、File）在 IDE 中启动，便于调试。

### 6.2 Nacos 配置管理

```properties
# 服务注册 + 动态配置
spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848
spring.cloud.nacos.config.server-addr=127.0.0.1:8848
```

---

## 7. 目录结构

```
eblog/
├── ARCHITECTURE.md              # 架构文档
├── docker-compose.yml           # 基础设施编排
├── back/
│   ├── services/
│   │   ├── gateway-service/     # Spring Cloud Gateway
│   │   │   ├── filter/          # JwtAuthGatewayFilter
│   │   │   ├── config/          # SentinelConfig, CorsConfig
│   │   │   └── handler/         # FallbackHandler
│   │   │
│   │   ├── auth-service/        # 认证服务
│   │   │   ├── controller/      # AuthController, OAuthController
│   │   │   ├── service/         # AuthService, OAuthService
│   │   │   ├── config/          # JwtProperties, KeyPairManager
│   │   │   └── util/            # JwtTokenProvider
│   │   │
│   │   ├── article-service/     # 文章服务（写模型）
│   │   │   ├── controller/      # Article, Draft, Comment, Upload
│   │   │   ├── service/         # ArticleService (Seata AT)
│   │   │   ├── entity/          # Article, Draft, Comment
│   │   │   └── mapper/          # MyBatis Mapper
│   │   │
│   │   ├── query-service/       # 文章查询服务（读模型）
│   │   │   ├── controller/      # ArticleQueryController
│   │   │   ├── service/         # ArticleQueryService
│   │   │   ├── dto/             # ArticleVO, PageResult
│   │   │   ├── mq/              # ArticleCacheEvictConsumer
│   │   │   └── mapper/          # ArticleQueryMapper
│   │   │
│   │   ├── search-service/      # 搜索服务
│   │   │   ├── controller/      # SearchController
│   │   │   ├── service/         # SearchService
│   │   │   └── document/        # ArticleDocument (ES mapping)
│   │   │
│   │   ├── comment-service/     # 评论服务
│   │   │   ├── controller/      # CommentController
│   │   │   ├── service/         # CommentService (Seata AT)
│   │   │   ├── entity/          # Comment (parentId)
│   │   │   └── dto/             # CommentVO, CreateCommentRequest
│   │   │
│   │   ├── notification-service/ # 通知服务
│   │   │   ├── controller/      # NotificationController
│   │   │   ├── service/         # NotificationService
│   │   │   ├── mq/              # CommentEventConsumer
│   │   │   └── mapper/          # NotificationMapper
│   │   │
│   │   └── file-service/        # 文件服务
│   │       ├── controller/      # FileController
│   │       ├── service/         # FileService, FileStorageService
│   │       ├── entity/          # FileRecord
│   │       └── mapper/          # FileRecordMapper
│   │
│   ├── common/
│   │   ├── common-core/         # Result, BizException, RsaKeyUtils
│   │   ├── common-dto/          # LoginRequestDTO, ArticleEventDTO
│   │   ├── common-feign/        # AuthClient, NotificationClient
│   │   └── common-cache/        # MultiCacheManager, BloomFilterManager
│   │
│   └── sql/
│       ├── init/                # 数据库初始化DDL
│       └── migration/           # 升级脚本
│
└── front/                       # React 前端
```