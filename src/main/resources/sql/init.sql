-- ===========================
-- 初始化数据库和表
-- ===========================
CREATE DATABASE IF NOT EXISTS `to_my_l`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `to_my_l`;

-- ===========================
-- 用户表（2个固定用户）
-- ===========================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(32)  NOT NULL,
    `gender`      TINYINT      DEFAULT 0,
    `username`    VARCHAR(64)  NOT NULL,
    `password`    varchar(256) NOT NULL,
    `birthday`       DATE         DEFAULT NULL,
    `bio`            TEXT         DEFAULT NULL,
    `is_first_login` TINYINT      DEFAULT 1,
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `user` (`name`, `gender`, `username`, `password`, `birthday`) VALUES
('王水群', 1, 'wangshuiqun', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '2006-07-29'),
('潘佩雪',   0, 'panpeixue',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '2006-07-29');

-- ===========================
-- 日记表
-- ===========================
DROP TABLE IF EXISTS `diary`;
CREATE TABLE `diary` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `content`     TEXT         NOT NULL,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- 朋友圈 (Moments)
-- ===========================
DROP TABLE IF EXISTS `moment`;
CREATE TABLE `moment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `content`     TEXT         NOT NULL,
    `image`       VARCHAR(512) DEFAULT NULL,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- 朋友圈点赞 (仅允许对方点赞)
-- ===========================
DROP TABLE IF EXISTS `moment_like`;
CREATE TABLE `moment_like` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `moment_id`   BIGINT   NOT NULL,
    `user_id`     BIGINT   NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_moment_user` (`moment_id`, `user_id`),
    KEY `idx_moment_id` (`moment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- 朋友圈评论
-- ===========================
DROP TABLE IF EXISTS `moment_comment`;
CREATE TABLE `moment_comment` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `moment_id`   BIGINT   NOT NULL,
    `user_id`     BIGINT   NOT NULL,
    `content`     TEXT     NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_moment_id` (`moment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- 留言 (不支持回复, 已读标识)
-- ===========================

-- ===========================
-- 聊天消息
-- ===========================
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `sender_name` VARCHAR(32)  NOT NULL,
    `content`     TEXT         NOT NULL,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `sender_id`   BIGINT       NOT NULL,
    `receiver_id` BIGINT       NOT NULL,
    `content`     TEXT         NOT NULL,
    `is_read`     TINYINT      DEFAULT 0,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_receiver` (`receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;