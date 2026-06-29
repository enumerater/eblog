-- ============================================================================
-- eblog 微服务 — 数据库初始化脚本
-- 用途: Docker MySQL 容器首次启动时自动执行
-- 放在 docker-compose.yml 的 ./sql/init 目录下
-- ============================================================================

-- 扩展服务数据库（每个微服务独立 schema）
CREATE DATABASE IF NOT EXISTS `my_blog` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `my_blog`;

-- ─── 查询服务依赖: 文章表 (与 article-service 共享) ───
-- articles 表已在业务代码中创建, 此处仅做参考
-- CREATE TABLE IF NOT EXISTS `articles` (...)

-- ─── 评论服务: 扩展的评论表 ───
CREATE TABLE IF NOT EXISTS `comments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID (支持嵌套回复)',
    `author` VARCHAR(50) NOT NULL COMMENT '评论者昵称',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID (登录用户)',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '评论者头像 URL',
    `status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT '状态: PENDING/APPROVED/REJECTED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表(扩展)';

-- ─── 搜索服务: 搜索日志 ───
CREATE TABLE IF NOT EXISTS `search_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `keyword` VARCHAR(200) NOT NULL COMMENT '搜索关键词',
    `result_count` INT NOT NULL DEFAULT 0 COMMENT '搜索结果数',
    `searched_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    PRIMARY KEY (`id`),
    KEY `idx_keyword` (`keyword`),
    KEY `idx_searched_at` (`searched_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索日志';

-- ─── 智能服务: 文章分析结果 ───
CREATE TABLE IF NOT EXISTS `article_analysis` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `summary` TEXT COMMENT '文章摘要',
    `keywords` VARCHAR(500) COMMENT '关键词(JSON数组)',
    `word_count` INT NOT NULL DEFAULT 0 COMMENT '总字数',
    `reading_time_minutes` INT NOT NULL DEFAULT 0 COMMENT '阅读时长(分钟)',
    `analyzed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '分析时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章分析结果';

-- ─── 智能服务: 推荐日志 ───
CREATE TABLE IF NOT EXISTS `recommendation_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `article_id` BIGINT NOT NULL COMMENT '源文章ID',
    `recommended_article_id` BIGINT NOT NULL COMMENT '推荐文章ID',
    `score` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '推荐分数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐日志';

-- ─── 通知服务 ───
CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID (0=广播)',
    `type` VARCHAR(30) NOT NULL COMMENT '类型: SYSTEM/COMMENT_REPLY/ARTICLE_MENTION',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联ID (如评论ID)',
    `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ─── 文件服务 ───
CREATE TABLE IF NOT EXISTS `file_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `original_name` VARCHAR(500) NOT NULL COMMENT '原始文件名',
    `stored_path` VARCHAR(1000) NOT NULL COMMENT '存储路径',
    `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
    `md5` VARCHAR(32) DEFAULT NULL COMMENT '文件MD5(去重用)',
    `storage_type` VARCHAR(20) NOT NULL DEFAULT 'LOCAL' COMMENT '存储类型: LOCAL/OSS',
    `uploader_id` BIGINT DEFAULT NULL COMMENT '上传者用户ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_md5` (`md5`),
    KEY `idx_uploader_id` (`uploader_id`),
    KEY `idx_mime_type` (`mime_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件记录表';

-- ─── 查询服务: 文章阅读量 (可选分离表) ───
CREATE TABLE IF NOT EXISTS `article_view_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `viewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    `ip` VARCHAR(45) DEFAULT NULL COMMENT '访问IP',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_viewed_at` (`viewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章浏览日志';