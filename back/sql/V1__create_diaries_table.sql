-- =============================================
-- Diary Module - Create diaries table
-- =============================================
-- Usage: mysql -u root -p my_blog < V1__create_diaries_table.sql

CREATE TABLE IF NOT EXISTS `diaries` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `date` DATE NOT NULL UNIQUE COMMENT '日记日期，一天一篇',
    `content` TEXT NOT NULL COMMENT 'Markdown 内容',
    `mood` VARCHAR(20) DEFAULT NULL COMMENT '心情 emoji',
    `weather` VARCHAR(50) DEFAULT NULL COMMENT '天气',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记表';