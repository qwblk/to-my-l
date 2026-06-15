-- V5：聊天表改造
--   旧：chat_message(id, sender_name, content, create_time)
--   新：chat_message(id, sender_id, receiver_id, content, create_time, deleted_at)
--
-- 把旧表 RENAME 留底，建新表，再按 sender_name → user.id 反查迁过去。
-- 项目就两个用户，receiver_id 取「不是 sender 的另一个 user.id」。
-- 旧表保留 1-2 周确认无问题后再 DROP（脚本里没写 DROP，自己手动）。

RENAME TABLE `chat_message` TO `chat_message_v1`;

CREATE TABLE `chat_message` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `sender_id`   BIGINT   NOT NULL,
    `receiver_id` BIGINT   NOT NULL,
    `content`     TEXT     NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at`  DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_pair_time` (`sender_id`, `receiver_id`, `create_time`, `id`),
    KEY `idx_receiver_time` (`receiver_id`, `create_time`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `chat_message`
    (`id`, `sender_id`, `receiver_id`, `content`, `create_time`)
SELECT
    o.id,
    s.id AS sender_id,
    (SELECT u2.id FROM `user` u2 WHERE u2.id <> s.id LIMIT 1) AS receiver_id,
    o.content,
    o.create_time
FROM `chat_message_v1` o
JOIN `user` s ON s.name = o.sender_name;