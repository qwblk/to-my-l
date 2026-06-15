-- V6：聊天消息支持图文/视频
ALTER TABLE `chat_message`
    ADD COLUMN `media_list` TEXT DEFAULT NULL
        COMMENT 'JSON 数组，每项 {type:"image"|"video", url, width?, height?, duration?}'
        AFTER `content`;
