-- V3：朋友圈支持图文/视频，加 media_list（JSON 文本）
ALTER TABLE `moment`
    ADD COLUMN `media_list` TEXT DEFAULT NULL
        COMMENT 'JSON 数组，每项 {type:"image"|"video", url, width?, height?, duration?}'
        AFTER `image`;
