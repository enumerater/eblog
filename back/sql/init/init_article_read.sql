-- ============================================================================
-- Article Query Service (读模型) 数据库初始化
-- 数据库: db_article_read
-- 反范式化设计: 冗余 + 额外字段, 专为查询优化
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `db_article_read`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `db_article_read`;

-- ─── 文章读模型 (反范式化宽表) ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS `article_read` (
    `id`            VARCHAR(32)  NOT NULL                COMMENT '文章ID',
    `title`         VARCHAR(200) NOT NULL                COMMENT '标题',
    `summary`       VARCHAR(500) DEFAULT NULL            COMMENT '摘要',
    `content`       LONGTEXT     DEFAULT NULL            COMMENT '内容(可选, 列表页可不加载)',
    `tags`          VARCHAR(500) DEFAULT NULL            COMMENT '标签(JSON数组)',
    `status`        TINYINT      NOT NULL DEFAULT 0      COMMENT '状态',
    `author_id`     BIGINT       NOT NULL                COMMENT '作者ID',
    `author_name`   VARCHAR(50)  DEFAULT NULL            COMMENT '冗余: 作者名',

    -- CIP 分析结果 (由内容智能管道填充)
    `keywords`      VARCHAR(300) DEFAULT NULL            COMMENT '关键词(逗号分隔)',
    `quality_score` INT          DEFAULT 0               COMMENT '质量评分(0-100)',
    `read_time`     INT          DEFAULT 0               COMMENT '预计阅读时间(分钟)',

    -- 统计
    `read_count`    INT          NOT NULL DEFAULT 0      COMMENT '阅读数',
    `comment_count` INT          NOT NULL DEFAULT 0      COMMENT '评论数',
    `like_count`    INT          NOT NULL DEFAULT 0      COMMENT '点赞数',

    -- 时间
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `published_at`  DATETIME     DEFAULT NULL            COMMENT '发布时间',

    PRIMARY KEY (`id`),
    KEY `idx_status_created` (`status`, `created_at` DESC),
    KEY `idx_quality` (`quality_score` DESC),
    KEY `idx_tags` (`tags`(100)),
    KEY `idx_keywords` (`keywords`(100)),
    FULLTEXT KEY `ft_search` (`title`, `summary`, `keywords`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章读模型(反范式化)';
