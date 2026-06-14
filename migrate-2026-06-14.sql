-- ============================================================================
-- to-my-l 增量迁移 —— 从最初部署版本升级到当前版本
--
-- 用法：
--   1. 强烈建议先备份：mysqldump -uroot -p to_my_l > backup.sql
--   2. 在 MySQL 客户端 USE to_my_l; 后执行整个文件
--      或：mysql -uroot -p to_my_l < migrate-2026-06-14.sql
--
-- 这些 ALTER 都是幂等的：同一段 SQL 重复跑会报「Duplicate column」，
-- 但不会破坏已有数据。如果你确定还没跑过，可以一次到底。
-- ============================================================================

USE `to_my_l`;

-- ----------------------------------------------------------------------------
-- 1. user：last_seen_at
-- ----------------------------------------------------------------------------
ALTER TABLE `user`
    ADD COLUMN `last_seen_at` DATETIME DEFAULT NULL
        COMMENT '用户最后一次活跃时间，用于离线追赶'
        AFTER `is_first_login`;

-- ----------------------------------------------------------------------------
-- 2. moment：media_list + deleted_at
-- ----------------------------------------------------------------------------
ALTER TABLE `moment`
    ADD COLUMN `media_list` TEXT DEFAULT NULL
        COMMENT 'JSON 数组，每项 {type:"image"|"video", url, width?, height?, duration?}'
        AFTER `image`;

ALTER TABLE `moment`
    ADD COLUMN `deleted_at` DATETIME DEFAULT NULL
        COMMENT '软删除时间。NULL=未删，非 NULL=已删'
        AFTER `create_time`;

-- ----------------------------------------------------------------------------
-- 3. moment_like / moment_comment：deleted_at
-- ----------------------------------------------------------------------------
ALTER TABLE `moment_like`
    ADD COLUMN `deleted_at` DATETIME DEFAULT NULL
        COMMENT '当 moment 软删时连同点赞一并打 deleted_at；点赞行不单独删'
        AFTER `create_time`;

ALTER TABLE `moment_comment`
    ADD COLUMN `deleted_at` DATETIME DEFAULT NULL
        COMMENT '当 moment 软删时连同评论一并打 deleted_at；评论不单独删'
        AFTER `create_time`;

-- ----------------------------------------------------------------------------
-- 4. diary：deleted_at
-- ----------------------------------------------------------------------------
ALTER TABLE `diary`
    ADD COLUMN `deleted_at` DATETIME DEFAULT NULL
        COMMENT '软删除时间。NULL=未删，非 NULL=已删'
        AFTER `create_time`;

-- ----------------------------------------------------------------------------
-- 5. chat_message：列名改造（不兼容）
--
-- 旧表字段：id, sender_name(VARCHAR), content, create_time
-- 新表字段：id, sender_id(BIGINT), receiver_id(BIGINT), content,
--          media_list, create_time, deleted_at
--
-- 下面这段会保留旧数据：根据 sender_name 反查 user.name -> user.id，
-- receiver_id 用「不是 sender 的另一个人」（项目就两个用户）。
-- ----------------------------------------------------------------------------

-- 5.1 备份旧表（出问题可以从 chat_message_old 还原）
RENAME TABLE `chat_message` TO `chat_message_old`;

-- 5.2 新建表
CREATE TABLE `chat_message` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `sender_id`   BIGINT   NOT NULL,
    `receiver_id` BIGINT   NOT NULL,
    `content`     TEXT     NOT NULL,
    `media_list`  TEXT     DEFAULT NULL
        COMMENT 'JSON 数组，每项 {type:"image"|"video", url, width?, height?, duration?}',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at`  DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_pair_time` (`sender_id`, `receiver_id`, `create_time`, `id`),
    KEY `idx_receiver_time` (`receiver_id`, `create_time`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5.3 把旧聊天迁移过来：sender_name -> sender_id；receiver_id = 另一个用户
INSERT INTO `chat_message`
    (`id`, `sender_id`, `receiver_id`, `content`, `create_time`)
SELECT
    o.id,
    s.id AS sender_id,
    (SELECT u2.id FROM `user` u2 WHERE u2.id <> s.id LIMIT 1) AS receiver_id,
    o.content,
    o.create_time
FROM `chat_message_old` o
JOIN `user` s ON s.name = o.sender_name;

-- 5.4 校对一下：两表行数应该相等。如果不等，看 chat_message_old 里有没有
--     sender_name 对不上 user.name 的脏数据。
-- SELECT (SELECT COUNT(*) FROM chat_message) AS new_count,
--        (SELECT COUNT(*) FROM chat_message_old) AS old_count;

-- 5.5 确认 OK 后可以删旧表（先不删，留两天保险）：
-- DROP TABLE `chat_message_old`;

-- ============================================================================
-- 完成
-- ============================================================================
