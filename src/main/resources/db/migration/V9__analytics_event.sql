-- ============================================================================
-- V9：轻量访问事件统计
--
-- 私有项目自用：只记录访问事件，不接第三方统计、不做设备指纹、
-- 不记录输入内容、不记录密码/token/cookie。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `analytics_event` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       DEFAULT NULL,
    `visitor_id`  VARCHAR(64)  DEFAULT NULL,
    `event_type`  VARCHAR(64)  NOT NULL,
    `path`        VARCHAR(255) DEFAULT NULL,
    `detail`      JSON         DEFAULT NULL,
    `ip`          VARCHAR(64)  DEFAULT NULL,
    `user_agent`  VARCHAR(512) DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_analytics_event_create_time` (`create_time`),
    KEY `idx_analytics_event_type_time` (`event_type`, `create_time`),
    KEY `idx_analytics_event_user_time` (`user_id`, `create_time`),
    KEY `idx_analytics_event_visitor_time` (`visitor_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
