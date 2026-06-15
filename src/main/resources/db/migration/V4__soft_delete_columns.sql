-- V4：moment / diary 系列加软删除列
ALTER TABLE `moment`
    ADD COLUMN `deleted_at` DATETIME DEFAULT NULL
        COMMENT '软删除时间。NULL=未删，非 NULL=已删'
        AFTER `create_time`;

ALTER TABLE `moment_like`
    ADD COLUMN `deleted_at` DATETIME DEFAULT NULL
        COMMENT '当 moment 软删时连同点赞一并打 deleted_at；点赞行不单独删'
        AFTER `create_time`;

ALTER TABLE `moment_comment`
    ADD COLUMN `deleted_at` DATETIME DEFAULT NULL
        COMMENT '当 moment 软删时连同评论一并打 deleted_at；评论不单独删'
        AFTER `create_time`;

ALTER TABLE `diary`
    ADD COLUMN `deleted_at` DATETIME DEFAULT NULL
        COMMENT '软删除时间。NULL=未删，非 NULL=已删'
        AFTER `create_time`;
