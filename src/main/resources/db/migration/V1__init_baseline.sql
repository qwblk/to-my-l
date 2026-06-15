-- ============================================================================
-- V1__init_baseline.sql —— 项目最初部署时的 schema + 种子数据
--
-- 这版本对应 git history 里 "后端基本完成" 那一版的 init.sql:
--   user / diary / moment / moment_like / moment_comment / message / chat_message
-- 不含后来加的：last_seen_at / media_list / deleted_at / chat_message 改造。
--
-- 用于：
--   - 全新空库部署：Flyway 从 V1 跑到最新 V*
--   - 已经按这版部署过的云库：把 baseline-version 设成 1，Flyway 跳过 V1，
--     只跑 V2 之后的迁移。
-- ============================================================================

-- ===========================
-- 用户表
-- ===========================
CREATE TABLE IF NOT EXISTS `user` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(32)  NOT NULL,
    `gender`         TINYINT      DEFAULT 0,
    `username`       VARCHAR(64)  NOT NULL,
    `password`       VARCHAR(256) NOT NULL,
    `birthday`       DATE         DEFAULT NULL,
    `bio`            TEXT         DEFAULT NULL,
    `is_first_login` TINYINT      DEFAULT 1,
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `user` (`name`, `gender`, `username`, `password`, `birthday`, `bio`) VALUES
('王水群', 1, 'wangshuiqun', '$2a$10$wGB/vYjxL17Q4oCObpzQv.G18eu7NPKF9m.pYalJbLURkpwdMFpDS', '2006-07-29', '写代码的时候心里想的全是你'),
('潘佩雪',   0, 'panpeixue',  '$2a$10$wGB/vYjxL17Q4oCObpzQv.G18eu7NPKF9m.pYalJbLURkpwdMFpDS', '2001-02-24', '一个温柔的人');

-- ===========================
-- 日记表
-- ===========================
CREATE TABLE IF NOT EXISTS `diary` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `title`       VARCHAR(128) DEFAULT '',
    `content`     TEXT         NOT NULL,
    `mood`        VARCHAR(32)  DEFAULT NULL,
    `weather`     VARCHAR(32)  DEFAULT NULL,
    `is_private`  TINYINT      DEFAULT 1,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- 朋友圈
-- ===========================
CREATE TABLE IF NOT EXISTS `moment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `content`     TEXT         NOT NULL,
    `image`       VARCHAR(512) DEFAULT NULL,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `moment_like` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `moment_id`   BIGINT   NOT NULL,
    `user_id`     BIGINT   NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_moment_user` (`moment_id`, `user_id`),
    KEY `idx_moment_id` (`moment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `moment_comment` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `moment_id`   BIGINT   NOT NULL,
    `user_id`     BIGINT   NOT NULL,
    `content`     TEXT     NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_moment_id` (`moment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- 留言
-- ===========================
CREATE TABLE IF NOT EXISTS `message` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `sender_id`   BIGINT       NOT NULL,
    `receiver_id` BIGINT       NOT NULL,
    `content`     TEXT         NOT NULL,
    `is_read`     TINYINT      DEFAULT 0,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_receiver` (`receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- 聊天记录（旧版结构，sender_name 字符串）
-- V5 会把它重建成 sender_id/receiver_id 版本。
-- ===========================
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `sender_name` VARCHAR(32)  NOT NULL,
    `content`     TEXT         NOT NULL,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
