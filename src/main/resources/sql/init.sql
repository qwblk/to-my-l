-- ===========================
-- to-my-l 完整初始化脚本
-- ===========================

CREATE DATABASE IF NOT EXISTS `to_my_l`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `to_my_l`;

-- ===========================
-- 用户表
-- ===========================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(32)  NOT NULL,
    `gender`         TINYINT      DEFAULT 0,
    `username`       VARCHAR(64)  NOT NULL,
    `password`       VARCHAR(256) NOT NULL,
    `birthday`       DATE         DEFAULT NULL,
    `bio`            TEXT         DEFAULT NULL,
    `is_first_login` TINYINT      DEFAULT 1,
    `last_seen_at`   DATETIME     DEFAULT NULL COMMENT '用户最后一次活跃时间，用于离线追赶',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 已有库可单独执行下面这条迁移：
-- ALTER TABLE `user`
--     ADD COLUMN `last_seen_at` DATETIME DEFAULT NULL COMMENT '用户最后一次活跃时间，用于离线追赶' AFTER `is_first_login`;

INSERT INTO `user` (`name`, `gender`, `username`, `password`, `birthday`, `bio`) VALUES
('王水群', 1, 'wangshuiqun', '$2a$10$wGB/vYjxL17Q4oCObpzQv.G18eu7NPKF9m.pYalJbLURkpwdMFpDS', '2006-07-29', '写代码的时候心里想的全是你'),
('潘佩雪',   0, 'panpeixue',  '$2a$10$wGB/vYjxL17Q4oCObpzQv.G18eu7NPKF9m.pYalJbLURkpwdMFpDS', '2001-02-24', '一个温柔的人');

-- ===========================
-- 日记表
-- ===========================
DROP TABLE IF EXISTS `diary`;
CREATE TABLE `diary` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `title`       VARCHAR(128) DEFAULT '',
    `content`     TEXT         NOT NULL,
    `mood`        VARCHAR(32)  DEFAULT NULL,
    `weather`     VARCHAR(32)  DEFAULT NULL,
    `is_private`  TINYINT      DEFAULT 1,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `deleted_at`  DATETIME     DEFAULT NULL COMMENT '软删除时间。NULL=未删，非 NULL=已删',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 已有库可单独执行下面这条迁移：
-- ALTER TABLE `diary`
--     ADD COLUMN `deleted_at` DATETIME DEFAULT NULL
--         COMMENT '软删除时间。NULL=未删，非 NULL=已删'
--         AFTER `create_time`;

-- 日记数据 (wagnshuiqun)
INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '刚开始', '不知道从什么时候开始，特别期待和他聊天。哪怕只是随便唠唠日常，都觉得时间过得很快。跟他待在一起、说话的时候整个人都很放松，这种感觉还挺难得的。暂时没想太多未来，就单纯觉得，能认识他、能说上话，就挺开心的。', 'calm', 'sunny', 0, '2026-05-26 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '又聊了好久', '今天又和他聊了好久，话题扯东扯西的，全程都很轻松。每次像这样畅快聊天的时候，都会忍不住偷偷开心，好像心里某个角落都变得软软的。可热闹过后又会忍不住多想，我好像只是他众多聊天对象里普通的一个。', 'happy', 'cloudy', 0, '2026-05-27 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '找到他了', '最近总忍不住留意他，一开始只是觉得相处起来很舒服，慢慢发现自己好像有点不一样了。没事就会下意识望向他的方向，听到他说话的声音，心情都会不自觉变好。大概是真的动心了吧。离我很近的时候还能闻到他身上的香味，不像是香水味，但是很让人着迷，欲罢不能。嘻嘻，在朋友圈找到他的抖音了，以后可以和他续火花了，开心😊😊，还有他生日发的朋友圈，我估算一下，他大概25岁，也没差多少嘛，就是不知道他会不会介意。', 'excited', 'sunny', 0, '2026-05-28 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '合拍', '今天聊了挺久，从琐事聊到喜好，意外地合拍。很多想法都能说到一块儿去，这种默契真的很难得。结束聊天之后，躺在床上还在回想对话内容，忍不住偷偷笑了。有点贪心，希望这样的时刻能多一点。', 'happy', 'sunny', 0, '2026-05-29 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '翻聊天记录', '今天没去店里，也没主动发消息。空下来的时候就会翻看聊天记录，一遍又一遍。明明没什么特别的内容，却总也看不够。开始忍不住好奇，他心里会怎么看待我呢？只是把我当做普通弟弟吗？', 'calm', 'rainy', 0, '2026-05-30 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '期待又失落', '他说他明天可能会放假，给我开心坏了，我本想约她出去玩，但是他说已经有安排了，只能作罢。至少今天能和他多聊聊天，也很开心了，只不过糟心的是明天还要考试，又见不到他，难免难过。', 'sad', 'cloudy', 0, '2026-05-31 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '心疼', '他今天的重庆一日游好像不那么理想，出去了一天除了睡了会觉，啥也没玩上，也没吃到什么好吃的，心疼他三秒钟，难得的假期就这么虚度了，晚上还回去的那么晚，连休息都休息不好😭😭😭', 'sad', 'rainy', 0, '2026-06-01 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '替他不值', '天呐，他们怎么能这么对姐姐，原本就是他们要去，却一直让姐姐开车，来回八个小时啊，还把姐姐一个人丢在那边的店里，得有多难熬啊，我真的替他感到不值，但是姐姐人还是太好了，也没有怪他们，毫无怨言，好喜欢姐姐', 'angry', 'sunny', 0, '2026-06-02 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '失眠', '晚上躺在床上，脑海里全是和他相处的画面。失眠了大半晚，一点都不觉得烦躁，反而带着一点甜甜的惆怅。喜欢这件事，真是让人又欢喜又无奈。', 'calm', 'cloudy', 0, '2026-06-03 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '差点说出口', '今天差点把心意说出口了，但是表达的这么明显，他不会发现了吧，但是他这个回答我不懂是什么意思，我感觉好像在说他单纯把我当弟弟，这算拒绝吗，我不太懂。偶尔会偷偷发呆，脑海里全是他的样子，好像又闻到了他身上香香的味道，太迷恋了。也会偷偷幻想，如果我们不止是朋友会是什么模样。可幻想过后又清醒过来，把这份小小的憧憬压在心底。', 'anxious', 'rainy', 0, '2026-06-04 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '想分享', '没什么特别的安排，只是心里总挂着一个人。看到好玩的东西，第一反应就是想分享给他。原来喜欢上一个人之后，生活里大大小小的碎片，都想和对方扯上关系。', 'calm', 'sunny', 0, '2026-06-05 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '盼消息', '一整天都在盼着能和他说上话。等到消息弹出的那一刻，嘴角下意识就扬起来了。打字的时候总要反复斟酌，怕说错话，又想多跟他聊一会儿。这种小心翼翼的心情，大概只有自己才懂。', 'anxious', 'cloudy', 0, '2026-06-06 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '心跳加速', '今天在店里，姐姐捧着我的脸和我说话，那时候我的心跳的很快，快要跳出来了，但是我和他说我暑假要回家他明显愣了一下，我以为是他心里有些失落，不禁有些小窃喜，原来他还是在乎我的嘛。今天晚上还和他打了两局游戏，很开心😊😊😊', 'excited', 'sunny', 0, '2026-06-07 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '从来都是我主动', '我突然意识到，从来都是我主动找他聊天，他几乎没有主动和我发过消息，但是平时聊天的时候明明开心，都是我的错觉吗，我问过豆包，让他分析了我们的聊天记录，他说姐姐对我有好感，平时能聊的来，但我觉得并不是呀，因为我想着他会不会对谁都这样，我在他心里并没有什么特别的，只不过我会主动找他聊天而已，换一个人来也是一样的，想到这，我的心情突然失落。也许她只是比较忙吧，我找理由安慰自己。', 'sad', 'rainy', 0, '2026-06-08 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '酸酸的', '无意间看到他主动去和别的男生搭话了，对方长相很出众，她主动的不像话，我从未见过。说实话心里挺不是滋味的，酸酸的。明明早就隐约知道他会偏向颜值更高的人，可亲眼看到还是会难受。找豆包聊了很多，他劝我放弃，体面退场及时止损，可他只是个人工智能，哪里懂爱，喜欢哪里是说放下就能放下的，算了，就这样吧，能偶尔说说话就已经挺好了。', 'sad', 'rainy', 0, '2026-06-09 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '放平心态', '刻意让自己放平心态，照常和他相处、聊天。表面看着没什么变化，心里却多了一层顾虑。不敢把心意说出口，怕连现在这样轻松聊天的关系都维持不住。就这样小心翼翼地，把心思藏得严严实实。', 'calm', 'cloudy', 0, '2026-06-10 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '嫉妒', '不开心🙁🙁🙁，为啥他每次看到帅弟弟就会特别热情的主动上去说话，人家路过他都会撇过头去看。我绞尽脑汁在找话题，都聊不起来，别人却能一句话不说他就凑上去说一大堆，羡慕嫉妒！！！！！真的有点打击到我了，或许他真的不喜欢我吧😭😭😭', 'angry', 'rainy', 0, '2026-06-11 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '恋爱脑', '翻了翻之前的聊天记录，看着那些说笑的内容，还是会忍不住嘴角上扬。喜欢这件事真的不受控制啊。一边享受着聊天的快乐，一边又会因为他的选择暗自内耗。不想逼自己放下，也不敢往前迈步，大概这就是暗恋最熬人的地方。今天刷到一个视频，讲的恋爱脑，我发现我和他说的一模一样，说我只是因为从小缺爱才回这样，也许他说的是对的吧，但是我不觉得我喜欢他只是因为他在我身边会对我好，谁会喜欢一个对自己爱搭不理的人呢？不论如何，我对他的心意是真的，不论他是否喜欢我。', 'sad', 'rainy', 0, '2026-06-12 22:00:00');

INSERT INTO `diary` (`user_id`, `title`, `content`, `mood`, `weather`, `is_private`, `create_time`) VALUES
(1, '今天完工', '我第一个想要部署上线的网站，用户只有我和他，今天打算把后端完工，不只能能不能成，敲代码的时候一直在不自觉的想他，虽然他还是不会主动找我，有点小失落，但是一想到他，莫名会很愉悦，说到这我又想见他了，但是这几天可能会很忙，又没时间找他了', 'grateful', 'sunny', 0, '2026-06-13 22:00:00');

-- ===========================
-- 朋友圈
-- ===========================
DROP TABLE IF EXISTS `moment`;
CREATE TABLE `moment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `content`     TEXT         NOT NULL,
    `image`       VARCHAR(512) DEFAULT NULL COMMENT 'deprecated, 老数据兼容字段',
    `media_list`  TEXT         DEFAULT NULL COMMENT 'JSON 数组，每项 {type:"image"|"video", url, width?, height?, duration?}',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `deleted_at`  DATETIME     DEFAULT NULL COMMENT '软删除时间。NULL=未删，非 NULL=已删',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# 已有库可单独执行下面这条迁移：
# ALTER TABLE `moment`
#     ADD COLUMN `media_list` TEXT DEFAULT NULL
#         COMMENT 'JSON 数组，每项 {type:"image"|"video", url, width?, height?, duration?}'
#         AFTER `image`;
# ALTER TABLE `moment`
#     ADD COLUMN `deleted_at` DATETIME DEFAULT NULL
#         COMMENT '软删除时间。NULL=未删，非 NULL=已删'
#         AFTER `create_time`;

INSERT INTO `moment` (`user_id`, `content`, `create_time`) VALUES (1, '今天姐姐捧着我的脸和我说话，心跳加速到爆表 😊', '2026-06-07 21:30:00');
INSERT INTO `moment` (`user_id`, `content`, `create_time`) VALUES (1, '有些人就像星星，远远看着就很美，靠近了反倒怕被灼伤', '2026-06-09 23:00:00');
INSERT INTO `moment` (`user_id`, `content`, `create_time`) VALUES (1, '第一个自己写的项目马上就要上线了，用户只有我和他两个人，这就够了', '2026-06-13 10:00:00');

DROP TABLE IF EXISTS `moment_like`;
CREATE TABLE `moment_like` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `moment_id`   BIGINT   NOT NULL,
    `user_id`     BIGINT   NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted_at`  DATETIME DEFAULT NULL COMMENT '当 moment 软删时连同点赞一并打 deleted_at；点赞行不单独删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_moment_user` (`moment_id`, `user_id`),
    KEY `idx_moment_id` (`moment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 已有库可单独执行下面这条迁移：
-- ALTER TABLE `moment_like`
--     ADD COLUMN `deleted_at` DATETIME DEFAULT NULL
--         COMMENT '当 moment 软删时连同点赞一并打 deleted_at；点赞行不单独删'
--         AFTER `create_time`;

-- panpeixue likes wagnshuiqun's first moment
INSERT INTO `moment_like` (`moment_id`, `user_id`) VALUES (1, 2);

DROP TABLE IF EXISTS `moment_comment`;
CREATE TABLE `moment_comment` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `moment_id`   BIGINT   NOT NULL,
    `user_id`     BIGINT   NOT NULL,
    `content`     TEXT     NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted_at`  DATETIME DEFAULT NULL COMMENT '当 moment 软删时连同评论一并打 deleted_at；评论不单独删',
    PRIMARY KEY (`id`),
    KEY `idx_moment_id` (`moment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 已有库可单独执行下面这条迁移：
-- ALTER TABLE `moment_comment`
--     ADD COLUMN `deleted_at` DATETIME DEFAULT NULL
--         COMMENT '当 moment 软删时连同评论一并打 deleted_at；评论不单独删'
--         AFTER `create_time`;

INSERT INTO `moment_comment` (`moment_id`, `user_id`, `content`, `create_time`) VALUES
(1, 2, '哈哈可爱', '2026-06-07 22:00:00'),
(3, 2, '加油 💪', '2026-06-13 10:30:00');

-- ===========================
-- 留言
-- ===========================
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

INSERT INTO `message` (`sender_id`, `receiver_id`, `content`, `is_read`, `create_time`) VALUES
(1, 2, '今天累不累呀', 1, '2026-06-07 20:00:00'),
(2, 1, '还好 你呢',     0, '2026-06-07 20:05:00'),
(1, 2, '明天见！',       0, '2026-06-12 23:30:00');

-- ===========================
-- 聊天记录
-- ===========================
DROP TABLE IF EXISTS `chat_message`;
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

-- 已有库如果要保留旧聊天数据，建议先备份旧 chat_message，再按需迁移 sender_name 到 sender_id。
-- 新表结构迁移示例（会删除旧聊天记录）：
-- DROP TABLE IF EXISTS `chat_message`;
-- CREATE TABLE `chat_message` (
--     `id`          BIGINT   NOT NULL AUTO_INCREMENT,
--     `sender_id`   BIGINT   NOT NULL,
--     `receiver_id` BIGINT   NOT NULL,
--     `content`     TEXT     NOT NULL,
--     `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     `deleted_at`  DATETIME DEFAULT NULL,
--     PRIMARY KEY (`id`),
--     KEY `idx_pair_time` (`sender_id`, `receiver_id`, `create_time`, `id`),
--     KEY `idx_receiver_time` (`receiver_id`, `create_time`, `id`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `chat_message` (`sender_id`, `receiver_id`, `content`, `create_time`) VALUES (1, 2, '早啊', '2026-06-13 10:05:00');
INSERT INTO `chat_message` (`sender_id`, `receiver_id`, `content`, `create_time`) VALUES (2, 1, '早~', '2026-06-13 10:06:00');
INSERT INTO `chat_message` (`sender_id`, `receiver_id`, `content`, `create_time`) VALUES (1, 2, '今天打算把后端的部分弄完', '2026-06-13 10:07:00');
INSERT INTO `chat_message` (`sender_id`, `receiver_id`, `content`, `create_time`) VALUES (2, 1, '加油加油', '2026-06-13 10:08:00');
INSERT INTO `chat_message` (`sender_id`, `receiver_id`, `content`, `create_time`) VALUES (1, 2, '做完之后我们就可以用了', '2026-06-13 10:10:00');
INSERT INTO `chat_message` (`sender_id`, `receiver_id`, `content`, `create_time`) VALUES (2, 1, '好呀 期待', '2026-06-13 10:12:00');
