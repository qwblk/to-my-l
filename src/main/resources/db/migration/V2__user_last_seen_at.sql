-- V2：跨设备同步 last_seen_at
ALTER TABLE `user`
    ADD COLUMN `last_seen_at` DATETIME DEFAULT NULL
        COMMENT '用户最后一次活跃时间，用于离线追赶'
        AFTER `is_first_login`;
