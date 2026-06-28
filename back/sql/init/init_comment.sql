-- ============================================================================
-- Comment Service 数据库初始化
-- 数据库: db_comment
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `db_comment`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `db_comment`;

-- ─── 评论主表 ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `comment` (
    `id`          VARCHAR(32)  NOT NULL                COMMENT 'UUID',
    `article_id`  VARCHAR(32)  NOT NULL                COMMENT '文章ID',
    `author`      VARCHAR(50)  NOT NULL                COMMENT '作者',
    `content`     TEXT         NOT NULL                COMMENT '评论内容',
    `parent_id`   VARCHAR(32)  DEFAULT NULL            COMMENT '父评论ID',
    `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0=待审核 1=已通过 2=已拒绝',
    `like_count`  INT          NOT NULL DEFAULT 0      COMMENT '点赞数',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_article_status` (`article_id`, `status`, `created_at`),
    KEY `idx_parent` (`parent_id`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ─── 闭包路径表 (Closure Table) ──────────────────────────────────────
-- 优势: 查询所有子评论/祖先评论只需一次索引扫描, 复杂度 O(1)
-- 劣势: 写入时需要额外插入路径记录
CREATE TABLE IF NOT EXISTS `comment_path` (
    `ancestor`    VARCHAR(32)  NOT NULL                COMMENT '祖先节点ID',
    `descendant`  VARCHAR(32)  NOT NULL                COMMENT '后代节点ID',
    `depth`       INT          NOT NULL DEFAULT 0      COMMENT '层级深度: 0=自身',
    PRIMARY KEY (`ancestor`, `descendant`),
    KEY `idx_descendant` (`descendant`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论闭包路径表';

-- ─── 评论审核队列 ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `comment_audit` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `comment_id`  VARCHAR(32)  NOT NULL                COMMENT '评论ID',
    `content`     TEXT         NOT NULL                COMMENT '原始内容',
    `spam_score`  DECIMAL(5,2) DEFAULT 0.00            COMMENT '垃圾评分(0-1)',
    `audit_status` TINYINT     NOT NULL DEFAULT 0      COMMENT '审核状态: 0=待审核 1=自动通过 2=需人工审核 3=垃圾',
    `reason`      VARCHAR(200) DEFAULT NULL            COMMENT '审核原因',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_comment` (`comment_id`),
    KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论审核队列';
