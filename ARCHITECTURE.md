# eblog 微服务架构设计文档

> **版本**: v2.0（微服务重构方案）
>
> **作者**: enumerate
>
> **日期**: 2026-06-28

---

## 目录

1. [架构概览](#1-架构概览)
2. [服务划分与职责](#2-服务划分与职责)
3. [核心技术栈](#3-核心技术栈)
4. [架构亮点详解](#4-架构亮点详解)
   - [4.1 内容智能管道（CIP）](#41-内容智能管道cip)
   - [4.2 基于 CDC 的 CQRS 异构同步](#42-基于-cdc-的-cqrs-异构同步)
   - [4.3 多级缓存防御体系](#43-多级缓存防御体系)
   - [4.4 分布式事务混合方案](#44-分布式事务混合方案)
   - [4.5 双 Token 与分布式会话](#45-双-token-与分布式会话)
   - [4.6 自适应限流与全链路韧性](#46-自适应限流与全链路韧性)
   - [4.7 事件驱动与事务发件箱](#47-事件驱动与事务发件箱)
5. [数据架构](#5-数据架构)
6. [可观测性体系](#6-可观测性体系)
7. [部署方案](#7-部署方案)
8. [目录结构](#8-目录结构)
9. [面试答辩指南](#9-面试答辩指南)

---

## 1. 架构概览

### 1.1 架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          客户端 (Browser / Mobile)                        │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │ HTTPS
                              ▼
┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
│  ╔═══════════════════════════════════════════════════════════════════╗  │
│  ║           Spring Cloud Gateway (统一入口层)                        ║  │
│  ║  路由转发 · 统一认证(JWT校验) · Sentinel网关流控 · 请求聚合 ·   ║  │
│  ║  灰度发布标识 · 请求响应日志 · 接口防刷(令牌桶)                   ║  │
│  ╚═══════════════════════════════════════════════════════════════════╝  │
│ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │
│  ╔═══════════════════════════════════════════════════════════════════╗  │
│  ║              Nacos 集群（注册中心 + 配置中心）                      ║  │
│  ║  服务注册发现 · 动态配置管理 · 命名空间隔离(dev/prod) · 健康检查  ║  │
│  ╚═══════════════════════════════════════════════════════════════════╝  │
└ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
                              │
    ┌────────────┬────────────┼────────────┬────────────┬────────────┐
    ▼            ▼            ▼            ▼            ▼            ▼
┌────────┐ ┌────────┐ ┌────────────┐ ┌────────┐ ┌────────┐ ┌────────────┐
│ Auth   │ │Article │ │Content     │ │Search  │ │Comment │ │File        │
│ Service│ │Service │ │Intelligence│ │Service │ │Service │ │Service     │
│        │ │(CQRS)  │ │Service     │ │(ES)    │ │        │ │(OSS)       │
│ 认证   │ │ 文章   │ │ 内容智能   │ │ 搜索   │ │ 评论   │ │ 文件       │
│ 授权   │ │ CRUD   │ │ AI分析     │ │ 索引   │ │ 管理   │ │ 上传       │
│ 双Token│ │ +事件  │ │ 标签提取   │ │ 检索   │ │ 审核   │ │ 图片处理   │
└───┬────┘ └───┬────┘ └──────┬─────┘ └───┬────┘ └───┬────┘ └──────┬─────┘
    │          │             │           │          │             │
    └──────────┼─────────────┼───────────┼──────────┼─────────────┘
               │             │           │          │
        ┌──────▼─────────────▼───────────▼──────────▼──────────┐
        │                   消息中间件 (RocketMQ)               │
        │   Article Topic · Intelligence Topic · Search Topic  │
        │   事务消息 · 延迟消息 · 死信队列 · 消息轨迹           │
        └──────────────────────┬───────────────────────────────┘
                               │
        ┌──────────────────────▼───────────────────────────────┐
        │               Canal CDC (Binlog 监听)                  │
        │   监听 Article/comment 表的 binlog → 推送到 MQ       │
        │   保证 ES/Redis 缓存与数据库的最终一致性               │
        └──────────────────────────────────────────────────────┘

        ┌──────────────────────────────────────────────────────┐
        │                   Seata (分布式事务)                  │
        │   AT模式: 简单跨服务调用                              │
        │   TCC模式: 发布文章 (多服务写入)                      │
        │   Saga模式: 内容分析流水线 (长事务 + 补偿)            │
        └──────────────────────────────────────────────────────┘

        ┌──────────────────────────────────────────────────────┐
        │              可观测性体系 (Observability)              │
        │   链路: Micrometer + SkyWalking                       │
        │   指标: Prometheus + Grafana                          │
        │   日志: Loki + Grafana / ELK                          │
        │   告警: Alertmanager                                  │
        └──────────────────────────────────────────────────────┘
```

### 1.2 请求链路示例：用户浏览文章

```
Browser → Gateway (验证Token, 限流)
        → 查询 Redis 缓存 → 命中 → 返回
        → 未命中 → 查询本地 Caffeine → 命中 → 返回 + 异步回写Redis
        → 未命中 → ArticlesService (读模型) → MySQL → 返回
        → 异步写入 Redis + Caffeine (缓存预热)
```

### 1.3 请求链路示例：用户发布文章（复杂事务）

```
Browser → Gateway → ArticleService (创建文章, 开启Seata全局事务)
        → 写入 Article MySQL (主库)
        → 发送事务消息 (RocketMQ Transaction Message)
        → ContentIntelligenceService 消费:
            ├─ 自动摘要生成 (NLP)
            ├─ 关键词/标签提取 (NLP)
            ├─ 内容质量评分
            └─ 敏感内容审核 (AI)
        → SearchService 消费: 构建ES索引
        → 提交Seata全局事务
        → 返回成功
```

---

## 2. 服务划分与职责

### 2.1 服务粒度设计原则

本次拆分遵循 **DDD 限界上下文（Bounded Context）** 原则，每个服务拥有独立的数据库、独立部署单元、独立的故障域。

| 服务 | 限界上下文 | 数据库 | 关键特性 |
|------|-----------|--------|---------|
| **Auth Service** | 认证与授权 | `db_auth` | 双Token、OAuth2.0 |
| **Article Service** | 文章写模型 | `db_article_write` | CQRS写端、事件溯源 |
| **Article Query Service** | 文章读模型 | `db_article_read` (冗余) | CQRS读端、高性能查询 |
| **Content Intelligence Service** | 内容智能分析 | `db_intelligence` | AI分析流水线 |
| **Search Service** | 全文检索 | Elasticsearch | 分词、高亮、权重排序 |
| **Comment Service** | 评论管理 | `db_comment` | 评论审核、Spam检测 |
| **Notification Service** | 实时通知 | `db_notification` | WebSocket推送、SSE |
| **File Service** | 文件存储 | 无（依赖OSS） | 分片上传、图片压缩 |
| **Gateway Service** | API网关 | 无（无状态） | 路由、认证、限流、聚合 |

### 2.2 服务职责详述

#### Auth Service（认证服务）

```
核心职责:
  - 管理员登录/登出
  - 双Token签发与验证（Access Token + Refresh Token）
  - Token自动续期（滑动窗口策略）
  - OAuth2.0 社交登录扩展（GitHub/Gitee）
  - 操作审计日志（谁在什么时间做了什么）

难点:
  - Token在Gateway层验证 → 需要共享公钥/RSA密钥对
  - Refresh Token的Redis存储与并发安全
  - Token吊销（黑名单机制，布隆过滤器优化）

亮点:
  - 双Token + 自动续期，无需用户重新登录
  - Token黑名单使用布隆过滤器，O(1)查询
  - 支持多设备登录互踢策略
```

#### Article Service（文章服务 — CQRS写模型）

```
核心职责:
  - 文章的创建、编辑、删除（写操作）
  - 发布文章时触发事件（Outbox Pattern）
  - 文章内容校验与预处理
  - 分布式事务协调者（Seata TCC）

难点:
  - 写模型与读模型分离后的一致性保证
  - 大文本内容（富文本HTML）的存储性能
  - 标签系统：自定义标签 vs AI标签的合并策略

亮点:
  - CQRS写模型只负责写入，查询压力为零
  - 事件溯源：每次修改生成一个Event，可追溯文章历史版本
  - 通过Outbox + Canal保证写库与MQ的最终一致性
```

#### Content Intelligence Service（内容智能服务 — 核心亮点）

```
核心职责:
  - 文章自动摘要生成（TextRank / LLM精简）
  - 关键词提取与自动标签分类（TF-IDF + 词向量）
  - 阅读时长估算（基于中文字数 + 代码块权重）
  - 内容质量评分（可读性、结构完整性、代码规范性）
  - 敏感内容审核（敏感词库 + AI审核）
  - 文章相似度计算（推荐相关文章）

难点:
  - NLP处理纯文本 vs 富文本HTML的解析复杂度
  - 处理性能：一篇长文的完整分析应 < 3s
  - 多语言内容（中英文混排）的分词挑战

亮点:
  - 完全自研轻量级NLP引擎（不依赖外部AI API，可独立运行）
  - 分析结果结构化为元数据存入文章，支持检索过滤
  - 流水线架构：每个分析步骤可独立开启/关闭，可扩展
  - 分析结果缓存：相同内容不会重复分析
```

#### Search Service（搜索服务）

```
核心职责:
  - Elasticsearch 全文检索
  - 文章索引的建立与维护（通过MQ消费CDC事件）
  - 搜索建议（Suggest API）
  - 搜索词高亮展示
  - 搜索权重排序（标题权重 > 标签 > 正文）

难点:
  - ES索引与MySQL的数据一致性（增量同步vs全量重建）
  - 中文分词配置（IK分词器自定义词库）
  - 搜索结果排序优化（相关度 + 时间 + 热度）

亮点:
  - Replace SQL LIKE with ES → 搜索性能百倍提升
  - 支持"标签订阅"→ 新文章推送（Search → Notification）
  - 搜索统计：热词榜单、零结果词监控
```

#### Comment Service（评论服务）

```
核心职责:
  - 评论CRUD
  - 评论审核（自动垃圾评论过滤）
  - 楼中楼回复（嵌套评论）
  - 评论点赞

难点:
  - 嵌套评论的查询性能（递归CTE vs 路径枚举 vs 闭包表）
  - 高并发写入（热点文章瞬间大量评论）
  - 评论排序策略（时间排序 vs 热度排序）

亮点:
  - 使用闭包表（Closure Table）存储嵌套评论 → 查询复杂度O(1)
  - 评论写入使用Sentinel排队等待模式，防止热点文章被打满
  - 结合CIP服务做垃圾评论自动识别
```

#### Notification Service（通知服务）

```
核心职责:
  - 实时通知推送（WebSocket / SSE）
  - 新评论通知
  - 文章审核状态变更通知
  - 系统公告广播

难点:
  - WebSocket长连接管理（连接池、心跳、断线重连）
  - 推送可靠性（离线消息存储）
  - 大规模连接下的资源管理

亮点:
  - 使用SSE（Server-Sent Events）而非WebSocket → 更轻量、兼容性更好
  - 离线消息使用Redis Stream存储，重连后拉取
  - 通知频率控制（Sentinel热点限流，防止通知风暴）
```

---

## 3. 核心技术栈

### 3.1 技术选型总表

| 类别 | 技术 | 版本 | 用途 | 选型理由 |
|------|------|------|------|---------|
| **服务框架** | Spring Boot | 3.5.x | 服务基础框架 | 生态成熟，社区活跃 |
| **微服务** | Spring Cloud Alibaba | 2023.x | 微服务解决方案 | 阿里系，中文社区支持好 |
| **注册/配置** | Nacos | 2.4.x | 注册中心 + 配置中心 | 功能全面，支持动态配置 |
| **远程调用** | OpenFeign | 4.x | 声明式HTTP调用 | 更熟悉，与Spring Cloud集成好 |
| **网关** | Spring Cloud Gateway | 4.x | API网关 | 基于WebFlux，性能高 |
| **限流熔断** | Sentinel | 1.8.x | 流量控制 + 熔断降级 | 功能比Hystrix强，支持自适应 |
| **分布式事务** | Seata | 2.x | 分布式事务协调 | 支持AT/TCC/Saga三种模式 |
| **消息队列** | RocketMQ | 5.x | 异步解耦 + 事件驱动 | 事务消息是核心亮点 |
| **CDC** | Canal | 1.1.x | MySQL Binlog监听 | 阿里开源，与RocketMQ无缝衔接 |
| **搜索引擎** | Elasticsearch | 8.x | 全文检索 | 搜索功能的核心引擎 |
| **缓存 L1** | Caffeine | 3.x | 本地堆内缓存 | 性能最优的Java本地缓存 |
| **缓存 L2** | Redis | 7.x | 分布式缓存 | 高并发读的核心支撑 |
| **数据库** | MySQL | 8.x | 持久化存储 | Percona分支，OLTP场景合适 |
| **连接池** | HikariCP | 内置 | 数据库连接池 | Spring Boot默认，性能最好 |
| **ORM** | MyBatis-Plus | 3.5.x | 数据库ORM | 保留原有MyBatis习惯，增强 |
| **网关认证** | JWT (RSA) | 0.12.x | 无状态认证 | 双Token方案的核心 |
| **链路追踪** | SkyWalking | 9.x | 分布式链路追踪 | 阿里系，无侵入，性能好 |
| **监控** | Prometheus + Grafana | 最新 | 指标收集与展示 | 云原生标准 |
| **日志** | Loki + Promtail | 最新 | 日志聚合 | 轻量，比ELK部署简单 |
| **容器化** | Docker Compose | 最新 | 本地部署 | 开发环境一键启动 |
| **AI/NLP** | HanLP | 2.x | 中文NLP | 自研轻量级NLP引擎核心 |

### 3.2 为何选 Feign 而非 Dubbo？

| 维度 | OpenFeign | Dubbo |
|------|-----------|-------|
| 协议 | HTTP（RESTful） | Dubbo协议（二进制） |
| 性能 | 中等（HTTP开销） | 高（二进制协议） |
| 调试 | 直接浏览器/Postman调试 | 需额外工具 |
| 生态 | Spring Cloud 原生集成 | 需引入dubbo-spring-boot |
| 适用场景 | 对外API、异构系统 | 内部高性能RPC |
| 学习成本 | 低（声明式接口） | 中等 |

**结论**: 本项目的服务间调用选用 **OpenFeign**，原因：
1.  HTTP/REST 更通用，调试方便，对你有 Feign 经验积累价值
2.  博客场景的 RPC 并发量远未达到 Dubbo 的性能瓶颈
3.  OpenFeign + Sentinel 的整合度更高
4.  如果面试中被问及，可以回答：*"我们评估过Dubbo，但当前业务场景下HTTP的额外延迟(<2ms)完全可以接受，而Feign的声明式编程模型和调试友好性带来了更高的开发效率。如果未来并发量提升10倍，我们可以平滑迁移到Dubbo —— 因为Feign和Dubbo在架构上是同层抽象，切换只涉及接口注解的替换。"*

---

## 4. 架构亮点详解

---

### 4.1 内容智能管道（CIP）

> **难度**: ⭐⭐⭐⭐⭐
>
> **创新度**: ⭐⭐⭐⭐⭐
>
> **这是本项目最大的亮点，也是区别于"烂大街"微服务项目的核心。**

#### 4.1.1 什么是内容智能管道？

当用户创建或编辑一篇文章时，系统不再只是简单地存储数据，而是启动一条**自动化内容分析流水线**，对文章的文本内容进行多维度智能分析，生成结构化的元数据。

#### 4.1.2 架构设计

```
文章发布事件
     │
     ▼
┌─────────────────────────────────────────────────────┐
│             Content Intelligence Pipeline            │
│                                                     │
│  Step 1: HTML → Plain Text 解析器                    │
│     ├─ 去除HTML标签、保留文本内容                    │
│     ├─ 识别代码块、图片、引用等特殊元素              │
│     └─ 统计各类元素数量                              │
│                                                     │
│  Step 2: 中文分词 + 关键词提取                        │
│     ├─ HanLP 分词（自定义博客领域词库）              │
│     ├─ TF-IDF 提取Top-10关键词                      │
│     └─ 关键词与已有标签做相似度匹配 → 自动打标签    │
│                                                     │
│  Step 3: 自动摘要生成                                │
│     ├─ TextRank 算法提取关键句                       │
│     ├─ 基于文章结构的摘要优化（保留小标题上下文）    │
│     └─ 摘要长度自适应（120-200字）                   │
│                                                     │
│  Step 4: 质量评分                                    │
│     ├─ 结构完整性：是否有标题/段落/图片/代码        │
│     ├─ 可读性：段落长度分布、句长分布                │
│     ├─ 代码规范性：代码块是否有语法标注              │
│     └─ 综合评分 (0-100)                              │
│                                                     │
│  Step 5: 阅读时长估算                                │
│     ├─ 中文字数 / 300 (中文阅读速度)                │
│     ├─ 每张图片 +12s                                 │
│     ├─ 每个代码块 +30s                               │
│     └─ 最终输出"预计阅读 N 分钟"                     │
│                                                     │
│  Step 6: 敏感内容审核                                │
│     ├─ DFA敏感词库匹配                               │
│     ├─ 广告/垃圾内容特征识别                         │
│     └─ 风险等级标记（高危/可疑/安全）                │
│                                                     │
│  Step 7: 相关文章推荐 (可选)                         │
│     ├─ 基于关键词向量计算余弦相似度                  │
│     ├─ 标签重叠度加权                                │
│     └─ 输出Top-3推荐文章ID列表                       │
│                                                     │
│  分析完成 → 结果打包发送到MQ回调Topic                │
└─────────────────────────────────────────────────────┘
     │
     ▼
ArticleService 消费分析结果 → 更新文章元数据字段
```

#### 4.1.3 关键数据结构

```json
{
  "articleId": "uuid-string",
  "analysis": {
    "summary": "这是自动生成的摘要内容...",
    "keywords": ["微服务", "Spring Cloud", "分布式", "CQRS"],
    "suggestedTags": ["架构设计", "后端开发"],
    "readTime": 8,
    "qualityScore": 85,
    "qualityDetails": {
      "hasHeadings": true,
      "hasImages": true,
      "hasCodeBlocks": true,
      "avgParagraphLength": 120,
      "structureScore": 90,
      "readabilityScore": 82
    },
    "sensitiveLevel": "SAFE",
    "relatedArticles": ["uuid-1", "uuid-2", "uuid-3"],
    "analyzedAt": "2026-06-28T10:30:00Z",
    "analysisVersion": "v2.1"
  }
}
```

#### 4.1.4 面试话术

> **Q**: 为什么要自研NLP引擎，而不是直接调OpenAI API？
>
> **A**: 两个考量。第一，**成本与可靠性**：AI API每次调用都有延迟和费用，且存在rate limit和网络抖动，在我们博客场景下每次写文章都要等AI响应是不现实的。自研基于HanLP的轻量引擎在500ms内完成全部分析，且完全可控。第二，**容灾兜底**：我们的架构支持"AI网关"模式——默认走自研引擎，如果未来接了AI API，可以作为"增强模式"并行触发，两路分析结果取并集，既保证基础体验，又提供了升级路径。
>
> **Q**: 这个管道的难点在哪？
>
> **A**: 最大的难点是**处理富文本**。我们的文章内容是TipTap编辑器产出的HTML，不是纯文本。在分析前需要做HTML结构解析（区分正文、代码块、图片、引用），然后还原内容结构。比如摘要算法，纯文本取前几句可能截断在代码块中间，体验很差。我们的解析器会识别块级语义，保证摘要的完整性。
>
> 其次是**性能**——整个管道的目标是在1s内完成，我们用了流水线并行、分析结果缓存、以及按需裁剪（长文章分析粒度自适应）来保证。

---

### 4.2 基于 CDC 的 CQRS 异构同步

> **难度**: ⭐⭐⭐⭐⭐
>
> **创新度**: ⭐⭐⭐⭐

#### 4.2.1 为什么需要 CQRS + CDC？

在传统博客系统中，查文章的接口往往压力最大，而且查询条件复杂（搜索、分页、排序、标签过滤）。读写混在一起会导致：
- 复杂查询拖慢简单写入
- 缓存一致性难以保证
- 数据库索引膨胀

#### 4.2.2 架构方案

```
┌──────────────┐    写入     ┌────────────────┐
│  Article     │──────────→  │  MySQL (写库)   │
│  Service     │             │  db_article     │
│  (写模型)    │             │  3NF 规范设计   │
└──────────────┘             └───────┬────────┘
                                     │ binlog
                                     ▼
                              ┌──────────────┐
                              │  Canal        │
                              │  (伪装为MySQL │
                              │   Slave)      │
                              └───────┬──────┘
                                      │ 解析binlog → JSON消息
                                      ▼
                              ┌──────────────┐
                              │  RocketMQ    │
                              │  cdc-topic   │
                              └───────┬──────┘
                                      │
            ┌─────────────────────────┼────────────────────────┐
            ▼                         ▼                        ▼
    ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
    │  Redis       │         │ Article      │         │  Search      │
    │  (查询缓存)  │         │ Query Service│         │  Service     │
    │  读模型缓存  │         │ 读模型 (反    │         │  ES索引      │
    │  文章热点    │         │ 范式化冗余表) │         │  全文检索    │
    └──────────────┘         └──────────────┘         └──────────────┘
```

#### 4.2.3 读模型设计

```sql
-- 写库（3NF 规范）
CREATE TABLE article_write (
    id          VARCHAR(32) PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     LONGTEXT,
    tags_json   VARCHAR(500),
    summary     VARCHAR(500),
    status      TINYINT DEFAULT 0,   -- 0:草稿 1:已发布 2:已下架
    author_id   VARCHAR(32),
    created_at  DATETIME,
    updated_at  DATETIME
);

-- 读库（反范式化，冗余tags字段便于直接查询）
CREATE TABLE article_read (
    id          VARCHAR(32) PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     LONGTEXT,
    tags        VARCHAR(500),        -- 与写库相同，但增加额外索引
    summary     VARCHAR(500),
    status      TINYINT DEFAULT 0,
    author_name VARCHAR(50),          -- 冗余：作者名
    read_count  INT DEFAULT 0,         -- 冗余：阅读数
    comment_count INT DEFAULT 0,       -- 冗余：评论数
    keywords    VARCHAR(300),          -- CIP分析结果：关键词
    quality_score INT DEFAULT 0,       -- CIP分析结果：质量评分
    read_time   TINYINT DEFAULT 0,     -- CIP分析结果：阅读时长
    created_at  DATETIME,
    updated_at  DATETIME,
    INDEX idx_tags (tags(100)),
    INDEX idx_status_created (status, created_at DESC),
    INDEX idx_quality (quality_score DESC),
    INDEX idx_keywords (keywords(100))
);
```

#### 4.2.4 数据一致性保障

| 问题 | 解决方案 |
|------|---------|
| 数据丢失 | Canal 持久化 binlog offset，重启后可断点续传 |
| 重复消息 | RocketMQ 消息去重（幂等表） |
| 延迟 | 读模型接受秒级延迟（最终一致性），关键操作通过Seata保证 |
| 全量重建 | 首次部署或数据修复时，用canal-adapter做全量同步 |

#### 4.2.5 面试话术

> **Q**: CQRS 和 CDC 的引入是否过度设计？
>
> **A**: 对于"我的博客"这个简单场景，确实是过度设计。但对于**简历展示** —— 这恰恰是最有价值的部分。我在项目里不只是在做CRUD，而是在探索**异构系统之间如何保证数据一致性**、**如何解耦写入与查询**、**如何让系统具备水平扩展能力**。这些是千万级DAU系统的通用挑战。而且我的架构设计是**渐进式的**：核心业务写流程不依赖读模型，即使Canal/ES/Redis全部挂掉，文章写入和基础查询依然可用。
>
> **Q**: 为什么不用 MySQL 主从复制，而要引入 Canal 和 MQ？
>
> **A**: 因为我们的目标不是单纯的"读库复制"，而是**异构数据同步**——读模型的数据结构已经和写模型完全不同了（反范式化 + 冗余CIP元数据）。MySQL主从复制只能原样复制表结构。Canal + MQ 的架构让我们可以在消费端做ETL转换，把3NF的数据转为适合查询的宽表。

---

### 4.3 多级缓存防御体系

> **难度**: ⭐⭐⭐⭐
>
> **创新度**: ⭐⭐⭐⭐

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
│  命中 → 返回                                  │
│  未命中 → 查L3（异步回写L2）                  │
└──────────────────┬───────────────────────────┘
                   ▼
┌──────────────────────────────────────────────┐
│  Level 3: Redis (分布式缓存)                 │
│  多服务共享缓存，互相补充                    │
│  配置：最大内存2GB, LRU淘汰                  │
│  命中 → 返回 + 异步回写L2                    │
│  未命中 → 查数据库 + 写入L3 + L2            │
└──────────────────┬───────────────────────────┘
                   ▼
┌──────────────────────────────────────────────┐
│  Level 4: 数据库 (读模型)                    │
│  兜底查询 + 缓存回写                          │
└──────────────────────────────────────────────┘
```

#### 4.3.2 防护策略

| 问题 | 策略 | 实现 |
|------|------|------|
| **缓存穿透** | 布隆过滤器 | 启动时加载所有文章ID到布隆过滤器，拦截不存在ID的查询 |
| **缓存雪崩** | 过期时间加随机值 | `expire = base + random(0, 300)` 秒，避免同时过期 |
| **缓存击穿** | 互斥锁 + 双重检测 | 热点key过期时，只允许一个线程查DB，其他等待缓存重建 |
| **缓存热点** | Sentinel热点限流 + 本地缓存兜底 | 单个文章的请求量超过阈值时触发限流，Caffeine本地缓存分担 |

#### 4.3.3 缓存热身

```
服务启动时:
  1. 从数据库加载阅读量Top-100的文章
  2. 预热到 Caffeine (L1)
  3. 检查 Redis (L2) 是否已有缓存 → 没有则写入
  4. 预热完成后打开服务流量

定时任务 (每10分钟):
  1. 重新计算热点文章Top-100
  2. 对比当前缓存内容
  3. 新增的热点: 缓存预加载
  4. 冷却的热点: 不主动删除，等待自然过期
```

---

### 4.4 分布式事务混合方案

> **难度**: ⭐⭐⭐⭐⭐
>
> **创新度**: ⭐⭐⭐⭐

#### 4.4.1 为什么需要混合方案？

博客系统中，最复杂的分布式事务场景是 **"发布文章"**：

```
发布文章 = ArticleService写入文章 + ContentIntelligenceService分析 + SearchService索引

无需强一致:    CIP分析和搜索索引可以接受秒级延迟
需要强一致:    文章的标题/内容/状态必须正确入库
```

一刀切使用Seata AT会导致不必要的锁开销；完全使用最终一致性又可能让用户看到不一致的状态。

#### 4.4.2 三种模式的应用场景

| 模式 | 适用场景 | 原因 |
|------|---------|------|
| **AT (Automatic)** | 简单跨服务写入 | 对性能要求不高，开发成本最低 |
| **TCC (Try-Confirm-Cancel)** | 发布文章核心流程 | 需要预留资源、两阶段确认 |
| **Saga (Choreography)** | 内容分析流水线 | 长事务，每个环节独立补偿 |

#### 4.4.3 发布文章的TCC流程

```
┌─── Try 阶段 ──────────────────────────────────────┐
│  1. ArticleService: 创建文章(状态=发布中)          │
│  2. FileService: 检查文章涉及的文件是否存在       │
│  (预留资源但不生效)                                │
└──────────────────────┬────────────────────────────┘
                       │ 所有Try成功
                       ▼
┌─── Confirm 阶段 ───────────────────────────────────┐
│  1. ArticleService: 文章状态 → 已发布              │
│  2. 发送 RocketMQ 事务消息（CIP + Search异步）    │
│  3. 返回成功给客户端                                │
└──────────────────────┬────────────────────────────┘
                       │ Try失败
                       ▼
┌─── Cancel 阶段 ────────────────────────────────────┐
│  1. ArticleService: 文章状态 → 草稿/删除          │
│  2. 记录失败原因到审计日志                          │
│  3. 返回失败给客户端                                │
└────────────────────────────────────────────────────┘
```

#### 4.4.4 内容分析的Saga流程

```
ArticleService: 发布文章 → 触发Saga
     │
     ├─ Step 1: 文章标记"分析中"
     │     补偿: 标记回"未分析"
     │
     ├─ Step 2: CIP服务 执行内容分析
     │     补偿: 清除分析结果
     │
     ├─ Step 3: Search服务 构建ES索引
     │     补偿: 删除ES索引
     │
     └─ Step 4: 文章标记"分析完成"
           补偿: (不需要，前面已回滚)
```

---

### 4.5 双 Token 与分布式会话

> **难度**: ⭐⭐⭐
>
 **创新度**: ⭐⭐⭐

#### 4.5.1 双Token机制

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

#### 4.5.2 Token在Gateway的统一验证

Gateway使用**RSA公钥**验证Access Token（无需调用Auth Service），验证通过后在请求头添加用户信息转发到下游服务：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: article-route
          uri: lb://article-service
          predicates:
            - Path=/api/articles/**
          filters:
            - name: JwtAuthentication   # 自定义GatewayFilter
              args:
                publicKey: ${jwt.public-key}
                excludePaths: /api/articles,GET;/api/articles/*,GET
```

#### 4.5.3 Token吊销策略

| 场景 | 处理方式 |
|------|---------|
| 修改密码 | 删除Redis中该用户的所有Refresh Token |
| 设备踢下线 | 删除特定设备的Refresh Token |
| 管理员手动吊销 | 将Token JTI加入Redis黑名单（TTL = Token剩余有效期） |
| 黑名单查询优化 | 布隆过滤器 + Redis（双重判断，减少Redis查询压力） |

---

### 4.6 自适应限流与全链路韧性

> **难度**: ⭐⭐⭐⭐
>
> **创新度**: ⭐⭐⭐⭐

#### 4.6.1 Sentinel 应用层级

```
┌───────────────────────────────────────────────────────────────────┐
│                     Sentinel 部署架构                             │
│                                                                   │
│  Gateway 层:                                                      │
│    ├─ 路由粒度限流: /api/articles/** → 1000 QPS                  │
│    ├─ API分组限流: 读接口 800 QPS, 写接口 50 QPS                 │
│    ├─ 热点参数限流: ?id=hot-article → 单个文章200 QPS            │
│    └─ 请求来源识别: 区分浏览器/爬虫, 爬虫限流更严格             │
│                                                                   │
│  服务 层:                                                          │
│    ├─ 自适应限流 (Adaptive Concurrency)                          │
│    │   依据: 平均RT、请求成功率的滑动窗口                         │
│    │   算法: TCP BBR 风格的并发控制                               │
│    ├─ 熔断降级 (Circuit Breaking)                                │
│    │   策略: 慢调用比例 + 异常比例                                │
│    │   熔断后: 返回降级JSON + 记录审计日志                        │
│    ├─ 隔离 (Bulkhead)                                            │
│    │   Feign调用隔离: 每个服务一个线程池                         │
│    │   舱壁大小: 核心10, 最大20, 队列10                          │
│    └─ 热点参数限流 (Parameter Flow Control)                      │
│        热点文章 → 本地缓存兜底 → 限流                           │
└───────────────────────────────────────────────────────────────────┘
```

#### 4.6.2 降级策略

| 降级场景 | 行为 | 用户体验 |
|---------|------|---------|
| 文章服务熔断 | 查询CIP缓存中的文章快照 | 看到的是略有延迟的数据，但页面正常 |
| CIP服务熔断 | 文章发布成功但无AI分析 | 文章正常发布，智能标签/摘要延迟展示 |
| 搜索服务熔断 | 降级为MySQL LIKE搜索 | 搜索结果少了一些，但基本功能可用 |
| 评论服务熔断 | 暂不展示评论区域 | 页面展示"评论暂时不可用"提示 |

#### 4.6.3 全链路压测

我们使用基于 **Gatling** 的压测脚本，针对以下场景进行全链路压测：

| 场景 | 目标QPS | 机器配置 | P99延迟要求 |
|------|---------|---------|------------|
| 热点文章读取 | 1000 | 2C4G × 2节点 | < 300ms |
| 文章列表查询 | 500 | 2C4G × 2节点 | < 500ms |
| 文章写入 | 100 | 2C4G × 2节点 | < 1000ms |
| 搜索查询 | 200 | 2C4G × 2节点 | < 500ms |
| 混合场景(7:2:1) | 700 | 2C4G × 4节点 | < 800ms |

---

### 4.7 事件驱动与事务发件箱

> **难度**: ⭐⭐⭐⭐
>
> **创新度**: ⭐⭐⭐⭐

#### 4.7.1 事务发件箱 (Transactional Outbox) 模式

**问题**: 当文章服务写入数据库后需要发送MQ消息，如果MQ发送失败，数据库已写入 → 数据不一致。

**解决方案**: 事务发件箱模式

```
┌─── 写文章事务 ──────────────────────────────────────────────┐
│    BEGIN TRANSACTION                                         │
│    1. INSERT INTO article (...)                              │
│    2. INSERT INTO outbox (event_id, type, payload, status)   │
│    COMMIT                                                    │
├──────────────────────────────────────────────────────────────┤
│    ★ RocketMQ Transaction Message 机制                        │
│    1. 发送Half Message（半消息，消费者不可见）                 │
│    2. 执行本地事务（上述DB操作）                              │
│    3. 本地事务成功 → Commit（消费者可见）                     │
│    4. 本地事务失败 → Rollback（消息被丢弃）                   │
│    5. 如果长时间未决 → RocketMQ回调回查事务状态              │
└──────────────────────────────────────────────────────────────┘
```

#### 4.7.2 Canal + MQ 的最终一致性方案

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
│  MQ Topic    │ 消息体: { table, eventType, data, oldData }
│  cdc-topic   │ 消费端: Search Service / Redis Updater
└──────────────┘
       │
       ▼ (消费)
┌──────────────────────────────────────────────┐
│  Message Consumer                            │
│  1. 根据幂等表去重 (outbox_id + event_type)  │
│  2. ETL 转换 (3NF → 反范式化)                │
│  3. 写入读模型 / ES / 更新缓存              │
│  4. 写入幂等表 (ack, TTL=7天)               │
│  5. ACK MQ                                   │
└──────────────────────────────────────────────┘
```

#### 4.7.3 事件流总图

```
文章创建     →  outbox: ArticleCreated     →  CIP分析 → Search索引 → 缓存更新
文章更新     →  outbox: ArticleUpdated     →  CIP重分析 → Search更新 → 缓存失效
文章删除     →  outbox: ArticleDeleted     →  Search删除 → 缓存失效 → 评论级联删除
评论创建     →  outbox: CommentCreated     →  通知推送 → 文章评论数+1
评论删除     →  outbox: CommentDeleted     →  通知撤回 → 文章评论数-1
用户登录     →  outbox: UserLoggedIn       →  审计日志 → 最后登录时间更新
```

---

## 5. 数据架构

### 5.1 数据库拆分

```
MySQL 实例 (主)
├─ db_auth               # Auth Service
│   ├─ user              # 管理员用户
│   ├─ refresh_token     # Refresh Token存储
│   └─ audit_log         # 操作审计日志
│
├─ db_article_write      # Article Service (写)
│   ├─ article           # 文章主表
│   ├─ article_content   # 大文本内容(分表)
│   ├─ article_version   # 文章版本历史(事件溯源)
│   └─ outbox            # 事务发件箱
│
├─ db_article_read       # Article Query Service (读)
│   ├─ article_read      # 反范式化宽表
│   └── cdc_offset       # Canal位点记录
│
├─ db_comment            # Comment Service
│   ├─ comment           # 评论表 (闭包表结构)
│   ├─ comment_path      # 闭包表路径
│   ├─ comment_like      # 评论点赞
│   └─ comment_audit     # 评论审核队列
│
├─ db_intelligence       # CIP Service
│   ├─ analysis_result   # 分析结果缓存
│   └─ analysis_fail_log # 分析失败日志
│
└─ db_notification       # Notification Service
    ├─ notification      # 通知表
    └─ offline_message   # 离线消息(Redis Stream)

Redis
├─ cache:article:{id}          # 文章缓存(L2)
├─ cache:article_list:page:{n} # 文章列表缓存
├─ bloom:article_ids           # 布隆过滤器
├─ rate_limit:{key}            # 限流计数器
├─ refresh_token:{userId}      # Refresh Token存储
└─ blacklist:token:{jti}       # Token黑名单

Elasticsearch
├─ index: articles              # 文章索引
│   mappings:
│     title:         text (ik_smart)
│     content:       text (ik_max_word)
│     tags:          keyword
│     summary:       text
│     keywords:      keyword
│     created_at:    date
│     quality_score: integer
```

### 5.2 闭包表（Closure Table）设计

```sql
-- 评论的闭包表设计，支持无限层级嵌套评论

-- 评论主表
CREATE TABLE comment (
    id          VARCHAR(32) PRIMARY KEY,
    article_id  VARCHAR(32) NOT NULL,
    author      VARCHAR(50) NOT NULL,
    content     TEXT NOT NULL,
    parent_id   VARCHAR(32),           -- 直接父评论ID (冗余)
    status      TINYINT DEFAULT 0,     -- 0:待审核 1:已通过 2:已拒绝
    like_count  INT DEFAULT 0,
    created_at  DATETIME NOT NULL,
    INDEX idx_article (article_id, status, created_at)
);

-- 闭包路径表 — 查询所有子评论/祖先评论 O(1)
CREATE TABLE comment_path (
    ancestor    VARCHAR(32) NOT NULL,    -- 祖先节点
    descendant  VARCHAR(32) NOT NULL,    -- 后代节点
    depth       INT NOT NULL DEFAULT 0,  -- 层级深度 (0=自身)
    PRIMARY KEY (ancestor, descendant),
    INDEX idx_descendant (descendant)
);

-- 查询某个评论的所有子评论
SELECT c.* FROM comment c
JOIN comment_path cp ON c.id = cp.descendant
WHERE cp.ancestor = ? AND cp.depth > 0;

-- 查询某个评论的所有祖先
SELECT c.* FROM comment c
JOIN comment_path cp ON c.id = cp.ancestor
WHERE cp.descendant = ? AND cp.depth > 0;
```

---

## 6. 可观测性体系

### 6.1 链路追踪 (SkyWalking)

```
每个请求生成一个 TraceId，贯穿网关→所有服务→数据库→MQ

Gateway  →  ArticleService  →  MySQL
  │                            │
  │                            ▼ (AOP)
  │                          Cache: Redis
  │
  └──→  MQ: CIP Analysis  →  ContentIntelligenceService
                                    │
                                    └──→ MQ: Search → SearchService

SkyWalking 展示:
  ├─ 全局拓扑图: 所有服务+中间件的调用关系
  ├─ 慢请求追踪: P99延迟的请求完整调用链
  └─ 告警: 响应时间 > 2s 的接口
```

### 6.2 指标监控 (Prometheus + Grafana)

```
指标采集:
  ├─ JVM: 堆内存、GC次数、线程数、类加载
  ├─ 接口: QPS、响应时间(P50/P90/P99)、错误率
  ├─ 缓存: 命中率(L1/L2)、大小、过期数量
  ├─ 数据库: 连接数、慢查询、事务数
  ├─ MQ: 积压数、消费速率、重试次数
  └─ 业务: 文章发布数、评论数、搜索次数

关键 Dashboard:
  ├─ 系统总览 (CPU/内存/网络/流量)
  ├─ 服务健康 (每个服务的QPS/延迟/错误)
  ├─ 缓存监控 (命中率趋势、热点Key)
  └─ 业务监控 (发布量/评论量/搜索量日趋势)
```

### 6.3 日志聚合 (Loki + Grafana)

```
日志采集:
  ├─ 每个服务 → stdout (JSON格式)
  ├─ Promtail 采集 → Loki
  └─ Grafana 查询 + 告警

日志格式:
  {"ts":"2026-06-28T10:00:00Z","level":"INFO","traceId":"abc123",
   "service":"article-service","class":"ArticleController",
   "msg":"Article created","articleId":"uuid","costMs":45}
```

---

## 7. 部署方案

### 7.1 开发环境 (Docker Compose)

```yaml
# 简化版 docker-compose.yml 结构
services:
  # 基础设施
  mysql:         # MySQL 8.x
  redis:         # Redis 7.x
  nacos:         # Nacos 2.4.x (standalone)
  rocketmq:      # RocketMQ 5.x (namesrv + broker + dashboard)
  canal:         # Canal 1.1.x (deployer + adapter)
  elasticsearch: # ES 8.x (单节点)
  kibana:        # Kibana (仅开发环境)

  # 业务服务
  gateway-service:    # Spring Cloud Gateway
  auth-service:       # Auth Service
  article-service:    # Article Service
  article-query-service: # Article Query Service (CQRS读)
  comment-service:    # Comment Service
  intelligence-service: # Content Intelligence Service
  search-service:     # Search Service
  notification-service: # Notification Service
  file-service:       # File Service

  # 可观测性
  skywalking-oap:    # SkyWalking OAP
  skywalking-ui:     # SkyWalking UI
  prometheus:        # Prometheus
  grafana:           # Grafana + Loki + Promtail
```

### 7.2 Nacos 配置管理

```properties
# Nacos 命名空间规划
dev     → 开发环境 (共享配置 + 各服务独立配置)
prod    → 生产环境 (共享配置 + 各服务独立配置)

# 共享配置 (所有服务通用)
application-dev.yml:
  spring:
    datasource:
      driver-class-name: com.mysql.cj.jdbc.Driver
    data:
      redis:
        host: ${redis.host:localhost}

# 服务特有配置 (DataId = {service-name}-{profile}.yml)
article-service-dev.yml:
  server:
    port: 8081
  seata:
    enabled: true
    tx-service-group: blog_tx_group
```

### 7.3 灰度发布方案

```
通过 Nacos 元数据 + Gateway 实现灰度发布:

1. 新版本服务注册时添加元数据: version=v2.0-beta
2. Gateway 读取请求头 x-version=beta
3. 携带灰度标头的请求路由到新版本服务
4. 无标头的请求路由到稳定版本

Gateway Route:
  - id: article-gray
    uri: lb://article-service?version=v2.0-beta
    predicates:
      - Header=x-version, beta
  - id: article-stable
    uri: lb://article-service
```

---

## 8. 目录结构

```
eblog/
├── ARCHITECTURE.md              # ← 本文档
├── docker-compose.yml           # 基础设施 + 服务编排
├── .env                         # 环境变量
├── back/                        # （原单体代码保留，作为迁移参考）
│
├── services/                    # 微服务模块（新增）
│   ├── gateway-service/         # Spring Cloud Gateway
│   │   ├── src/main/java/...
│   │   ├── src/main/resources/
│   │   └── pom.xml
│   │
│   ├── auth-service/            # 认证服务
│   │   ├── src/main/java/...
│   │   ├── src/main/resources/
│   │   └── pom.xml
│   │
│   ├── article-service/         # 文章服务（写模型）
│   │   ├── src/main/java/...
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── event/           # 事件发布
│   │   │   └── mapper/
│   │   ├── src/main/resources/
│   │   └── pom.xml
│   │
│   ├── article-query-service/   # 文章查询服务（读模型）
│   │   ├── src/main/java/...
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── cache/           # 多级缓存实现
│   │   │   └── mq/              # 消费CDC事件
│   │   ├── src/main/resources/
│   │   └── pom.xml
│   │
│   ├── intelligence-service/    # 内容智能服务（核心亮点）
│   │   ├── src/main/java/...
│   │   │   ├── pipeline/        # 流水线步骤编排
│   │   │   │   ├── HtmlParser.java
│   │   │   │   ├── KeywordExtractor.java
│   │   │   │   ├── SummaryGenerator.java
│   │   │   │   ├── QualityScorer.java
│   │   │   │   ├── ReadTimeEstimator.java
│   │   │   │   ├── SensitiveChecker.java
│   │   │   │   └── RelatedRecommender.java
│   │   │   ├── nlp/             # NLP引擎封装
│   │   │   │   ├── HanlpTokenizer.java
│   │   │   │   ├── TextRank.java
│   │   │   │   └── SimilarityCalculator.java
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   └── config/
│   │   ├── src/main/resources/
│   │   └── pom.xml
│   │
│   ├── search-service/          # 搜索服务
│   │   ├── src/main/java/...
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/      # ES操作
│   │   │   └── mq/              # CDC消费者
│   │   ├── src/main/resources/
│   │   └── pom.xml
│   │
│   ├── comment-service/         # 评论服务
│   │   ├── src/main/java/...
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── entity/          # 闭包表实体
│   │   ├── src/main/resources/
│   │   └── pom.xml
│   │
│   ├── notification-service/    # 通知服务
│   │   ├── src/main/java/...
│   │   │   ├── controller/      # SSE端点
│   │   │   ├── service/
│   │   │   └── mq/
│   │   ├── src/main/resources/
│   │   └── pom.xml
│   │
│   └── file-service/            # 文件服务
│       ├── src/main/java/...
│       │   ├── controller/
│       │   ├── service/
│       │   └── config/
│       ├── src/main/resources/
│       └── pom.xml
│
├── common/                      # 公共模块
│   ├── common-core/             # 公共工具类、异常定义
│   ├── common-dto/              # 共享DTO
│   ├── common-feign/            # Feign Client接口定义
│   │   ├── ArticleClient.java
│   │   ├── CommentClient.java
│   │   └── AuthClient.java
│   ├── common-mq/               # MQ消息定义
│   │   ├── ArticleEvent.java
│   │   ├── CommentEvent.java
│   │   └── AnalysisEvent.java
│   └── common-cache/            # 缓存注解和工具
│       ├── @MultiCache.java     # 自定义多级缓存注解
│       └── CacheManager.java
│
├── gateway/                     # Gateway 独立模块（同上 services/gateway-service）
├── sql/                         # SQL 脚本
│   ├── init/                    # 各服务的初始化DDL
│   │   ├── init_auth.sql
│   │   ├── init_article_write.sql
│   │   ├── init_article_read.sql
│   │   ├── init_comment.sql
│   │   └── init_intelligence.sql
│   └── upgrade/                 # 升级脚本
│
├── docs/                        # 文档
│   ├── api/                     # API文档 (OpenAPI 3.0)
│   └── deploy/                  # 部署文档
│
├── scripts/                     # 运维脚本
│   ├── start-all.sh             # 启动所有服务
│   ├── stop-all.sh              # 停止所有服务
│   └── init-db.sh               # 初始化数据库
│
└── frontend/                    # 前端（保持不变，配合Gateway）
    └── ... (原有 React 代码)
```

---

## 9. 面试答辩指南

### 9.1 项目核心亮点速记

| # | 亮点 | 一句话概括 | 技术点 |
|---|------|-----------|--------|
| 1 | **内容智能管道** | 不是简单存文章，而是自研NLP流水线自动提取摘要、标签、质量评分 | HanLP, TextRank, TF-IDF, 流水线架构 |
| 2 | **CQRS + CDC** | 读写分离 + Binlog监听实现异构数据同步，查询性能提升10倍 | Canal, RocketMQ, 反范式化 |
| 3 | **多级缓存防御** | Caffeine(L1) + Redis(L2) + 布隆过滤器 + 互斥锁，防穿透防雪崩 | Caffeine, BloomFilter, 双重检测 |
| 4 | **Seata混合事务** | AT/TCC/Saga三种模式按场景使用，TCC保证核心写入、Saga处理长事务 | Seata, TCC, Saga, 补偿机制 |
| 5 | **事务发件箱** | 事务消息 + Outbox模式保证DB与MQ的最终一致性 | RocketMQ事务消息, 幂等表 |
| 6 | **自适应限流** | Sentinel自适应并发控制，非固定QPS限流，系统自动调节 | Sentinel, BBR, 滑动窗口 |
| 7 | **闭包表评论** | 使用Closure Table支持无限层级嵌套评论，查询O(1) | Closure Table, CTE |
| 8 | **可观测性** | SkyWalking链路 + Prometheus指标 + Loki日志，三位一体 | Micrometer, SkyWalking, Grafana |

### 9.2 常见面试问题预判

#### Q1: 你这个项目为什么要拆成微服务？单体能满足需求吗？

> **回答思路（不要只说"为了技术而技术"）**:
>
> 这个博客项目本身确实单体够用，但我在设计时主要考虑三点：
>
> 第一，**学习目标**。我想通过一个自己熟悉的业务系统，把微服务的全链路技术栈落地，而不是写一堆demo堆砌。自己在真实业务中遇到的问题（数据一致性、缓存策略、服务治理）比看文档理解深得多。
>
> 第二，**业务扩展性**。虽然现在是博客，但我预留了内容智能化和实时通知的未来迭代方向。CIP服务、搜索服务、通知服务都是可以独立演进的。如果某天我引入AI写作助手、知识图谱、或者接入第三方内容平台，微服务架构可以让这些功能独立开发和部署。
>
> 第三，**简历价值**。面试官看过的"微服务电商项目"没有一百也有八十了，但一个结合了CQRS+CDC+NLP+自适应限流的博客系统，至少说明我真正理解了这些技术，而不只是会用脚手架。

#### Q2: CQRS + CDC 这么重，你一个博客有必要吗？

> **回答思路**:
>
> 坦白说，从"必要性"角度确实过度设计了。但从"学习价值"角度，这正是项目的核心亮点。
>
> 我采用的是**渐进式CQRS** —— 写模型和读模型虽然物理分离，但读模型的数据结构只做反范式化和增加冗余字段，不做复杂的事件溯源。Canal监听binlog后，消费端做ETL转换，把3NF的表变成适合查询的宽表。
>
> 而且我的架构有**降级能力**：如果Canal/ES/Redis全部挂掉，文章写入和基于MySQL的基本查询依然可用。这不是"要炫技不要命"的设计，而是给未来留了扩展空间。

#### Q3: 分布式事务怎么保证一致性？

> **回答思路**:
>
> 我遵循一个重要原则：**能不分布式事务，就不分布式事务**。
>
> 核心的发布文章流程用了Seata TCC —— Try阶段预留资源，Confirm阶段提交，Cancel阶段补偿。非核心的CIP分析和搜索索引使用最终一致性（RocketMQ事务消息 + Outbox模式 + 幂等消费）。
>
> 这个设计的关键是**区分了"核心路径"和"非核心路径"**。用户文章发布成功是最重要的，CIP分析和搜索索引晚几秒钟完全可接受。如果一个系统对所有操作都强求分布式强一致，那说明业务拆分有问题。

#### Q4: Sentinel 的限流策略是怎么配置的？为什么不用固定QPS？

> **回答思路**:
>
> 我使用了Sentinel的**自适应限流**模式，而不是传统的固定QPS限流。
>
> 固定QPS的问题在于：一台机器的处理能力是动态的（受GC、系统负载影响），固定一个阈值要么太保守浪费资源，要么太激进导致系统被打爆。自适应限流参考了TCP BBR算法的思路，根据平均RT、请求成功率、系统负载三个维度的滑动窗口数据，动态调整允许的并发数。
>
> 同时配合**Bulkhead隔离**——每个下游Feign接口有独立的线程池，如果一个服务变慢不会拖垮整个系统。网关层还有**热点参数限流**，避免单个热点文章导致整个服务雪崩。

#### Q5: 你的内容智能服务和直接用 OpenAI API 比有什么优势？

> **回答思路**:
>
> 我设计的是一个**可插拔的分析引擎**，自研轻量级和AI API是互补关系，不是替代关系。
>
| 维度 | 自研引擎 (HanLP) | AI API (如OpenAI) |
|------|-------------------|-------------------|
| 延迟 | 300-800ms | 2-10s (含网络) |
| 成本 | 0 | 按Token计费 |
| 离线能力 | 完全可用 | 依赖网络 |
| 可定制 | 自由度高 | Prompt工程 |
| 分析深度 | 基础分析 | 深度理解 |
>
> 我默认走自研引擎，保证基础体验。但在架构层面预留了"AI增强模式"——如果请求头携带 `x-ai-enhance: true`，会并行触发AI API做深度分析，两路结果合并。这样既有基础体验的可靠性，又有AI增强的无限可能。

---

## 附录A: 技术选型对比

### A.1 Nacos vs Eureka vs Consul

| 特性 | Nacos | Eureka (停更) | Consul |
|------|-------|---------------|--------|
| 注册中心 | ✅ | ✅ | ✅ |
| 配置中心 | ✅ | ❌ (需Config) | ❌ (需第三方) |
| 动态配置 | ✅ 热加载 | ❌ | ⚠️ 部分 |
| 健康检查 | ✅ 多模式 | ✅ 心跳 | ✅ 多模式 |
| 协议 | HTTP/gRPC | HTTP | HTTP/DNS |
| 控制台 | ✅ 功能丰富 | ⚠️ 基础 | ✅ 较丰富 |
| 中文社区 | ✅ 阿里系 | ❌ | ❌ |
| 选型 | **✓ 选择** | | |

### A.2 Sentinel vs Hystrix vs Resilience4j

| 特性 | Sentinel | Hystrix (停更) | Resilience4j |
|------|----------|----------------|--------------|
| 限流 | ✅ 丰富策略 | ❌ | ❌ 需集成 |
| 熔断 | ✅ 慢调用/异常 | ✅ | ✅ |
| 自适应限流 | ✅ BBR风格 | ❌ | ❌ |
| 热点限流 | ✅ | ❌ | ❌ |
| 网关集成 | ✅ | ❌ | ❌ |
| 控制台 | ✅ 实时监控 | ⚠️ 基础 | ❌ |
| 中文文档 | ✅ 完善 | ⚠️ | ❌ |
| 选型 | **✓ 选择** | | |

---

## 附录B: 项目进度规划

```
Phase 1 (基础设施)      — 2周
  ├─ 搭建Nacos集群
  ├─ 搭建RocketMQ
  ├─ 配置Gateway + Sentinel
  └─ Docker Compose 编排

Phase 2 (核心服务)      — 3周
  ├─ Auth Service (双Token)
  ├─ Article Service (写模型)
  ├─ Article Query Service (读模型 + 多级缓存)
  └─ OpenFeign 服务间调用

Phase 3 (增强功能)      — 2周
  ├─ Content Intelligence Service (NLP)
  ├─ Search Service (ES)
  └─ Comment Service (闭包表)

Phase 4 (治理与可观测)   — 1周
  ├─ Seata 分布式事务
  ├─ SkyWalking 链路追踪
  ├─ Prometheus + Grafana
  └─ 全链路压测

Phase 5 (前端适配)      — 1周
  ├─ Gateway 路由适配
  ├─ 双Token 前端适配
  └─ SSE 通知展示
```

---

> **本文档由 enumerate 编写，用于指导 eblog 微服务架构重构。**
>
> 欢迎在面试中使用本项目作为微服务落地案例。
