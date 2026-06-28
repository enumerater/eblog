-- ============================================================================
-- Auth Service 数据库初始化
-- 数据库: db_auth
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `db_auth`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `db_auth`;

-- ─── 管理员用户表 ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`      VARCHAR(50)  NOT NULL                COMMENT '用户名',
    `password`      VARCHAR(100) NOT NULL                COMMENT '密码',
    `role`          VARCHAR(20)  NOT NULL DEFAULT 'admin' COMMENT '角色: admin/editor',
    `is_enabled`    TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '启用状态: 0禁用 1启用',
    `last_login_ip` VARCHAR(45)  DEFAULT NULL             COMMENT '最后登录IP',
    `last_login_at` DATETIME     DEFAULT NULL             COMMENT '最后登录时间',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员用户';

-- 插入默认管理员 (密码: 1234)
INSERT INTO `user` (`username`, `password`, `role`, `is_enabled`)
VALUES ('admin', '1234', 'admin', 1)
ON DUPLICATE KEY UPDATE `username` = `username`;

-- ─── 操作审计日志表 ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       NOT NULL                COMMENT '用户ID',
    `username`      VARCHAR(50)  NOT NULL                COMMENT '用户名',
    `action`        VARCHAR(100) NOT NULL                COMMENT '操作类型: LOGIN/LOGOUT/CREATE/UPDATE/DELETE',
    `target_type`   VARCHAR(50)  DEFAULT NULL            COMMENT '操作对象类型: Article/Comment/User',
    `target_id`     VARCHAR(32)  DEFAULT NULL            COMMENT '操作对象ID',
    `detail`        VARCHAR(500) DEFAULT NULL            COMMENT '操作详情',
    `ip`            VARCHAR(45)  DEFAULT NULL            COMMENT '请求IP',
    `user_agent`    VARCHAR(500) DEFAULT NULL            COMMENT '用户代理',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_action` (`action`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';
