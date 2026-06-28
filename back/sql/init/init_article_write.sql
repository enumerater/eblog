-- ============================================================================
-- Article Service (写模型) 数据库初始化
-- 数据库: db_article_write
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `db_article_write`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `db_article_write`;

-- ─── 文章主表 ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `article` (
    `id`          VARCHAR(32)  NOT NULL                COMMENT 'UUID',
    `title`       VARCHAR(200) NOT NULL                COMMENT '标题',
    `summary`     VARCHAR(500) DEFAULT NULL            COMMENT '摘要',
    `author_id`   BIGINT       NOT NULL                COMMENT '作者ID',
    `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0=草稿 1=已发布 2=已下架',
    `tags_json`   VARCHAR(500) DEFAULT NULL            COMMENT '标签JSON数组',
    `version`     INT          NOT NULL DEFAULT 1      COMMENT '版本号(乐观锁, CQRS事件溯源)',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_author` (`author_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章主表(写模型)';

-- ─── 文章内容表 (垂直分表: 大文本单独存储) ──────────────────────────
CREATE TABLE IF NOT EXISTS `article_content` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `article_id` VARCHAR(32)  NOT NULL                COMMENT '文章ID',
    `content`    LONGTEXT     NOT NULL                COMMENT '富文本HTML内容',
    `content_md` LONGTEXT     DEFAULT NULL            COMMENT 'Markdown内容(可选)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章内容(垂直分表)';

-- ─── 文章版本历史 (CQRS 事件溯源) ───────────────────────────────────
CREATE TABLE IF NOT EXISTS `article_version` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `article_id` VARCHAR(32)  NOT NULL                COMMENT '文章ID',
    `version`    INT          NOT NULL                COMMENT '版本号',
    `title`      VARCHAR(200) NOT NULL                COMMENT '标题',
    `content`    LONGTEXT     NOT NULL                COMMENT '内容',
    `tags_json`  VARCHAR(500) DEFAULT NULL            COMMENT '标签',
    `summary`    VARCHAR(500) DEFAULT NULL            COMMENT '摘要',
    `operator`   VARCHAR(50)  NOT NULL                COMMENT '操作人',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_article_ver` (`article_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章版本历史(事件溯源)';

-- ─── 事务发件箱 (Transactional Outbox) ──────────────────────────────
CREATE TABLE IF NOT EXISTS `outbox` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `event_id`   VARCHAR(64)  NOT NULL                COMMENT '事件UUID',
    `event_type` VARCHAR(100) NOT NULL                COMMENT '事件类型: ArticleCreated/Updated/Deleted',
    `aggregate_type` VARCHAR(50) NOT NULL             COMMENT '聚合类型: Article',
    `aggregate_id`   VARCHAR(32) NOT NULL             COMMENT '聚合ID',
    `payload`    JSON         NOT NULL                COMMENT '事件载荷',
    `status`     TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0=待发送 1=已发送 2=发送失败',
    `retry_count` INT         NOT NULL DEFAULT 0      COMMENT '重试次数',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `sent_at`    DATETIME     DEFAULT NULL            COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_aggregate` (`aggregate_type`, `aggregate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务发件箱';
