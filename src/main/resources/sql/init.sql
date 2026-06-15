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
-- 2026-05-26
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-05-26 刚开始',
        '不知道从什么时候开始，特别期待和他聊天。哪怕只是随便唠唠日常，都觉得时间过得很快。

        跟他待在一起、说话的时候，整个人都很放松。这种感觉其实很难得。没有刻意找话题的紧张，也没有担心冷场的拘束，好像只要待在他的身边，就会不自觉地放松下来。

        暂时没想太多未来，也没有想过会不会有结果。只是单纯觉得，能认识他、能说上话，就已经是一件很开心的事情了。

        原来喜欢的开始，不一定轰轰烈烈。有时候只是某个平凡的瞬间，你忽然意识到，自己开始期待见到一个人。',
        '喜欢', 'sunny', 0, '2026-05-26 21:13:42', NULL);

-- 2026-05-27
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-05-27 又聊了好久',
        '今天又和他聊了很久，话题从工作聊到吃的，又从兴趣爱好扯到各种乱七八糟的事情，全程都很轻松。

        每次像这样畅快聊天的时候，我都会忍不住偷偷开心，好像心里某个角落都变得软软的。

        可热闹过后，又会忍不住多想。

        我好像只是他众多聊天对象里普通的一个。那些让我记挂很久的对话，对他来说，会不会只是再普通不过的一天？

        喜欢一个人之后，连快乐都会夹杂着一点不安。',
        '喜欢', 'cloudy', 0, '2026-05-27 20:45:17', NULL);

-- 2026-05-28
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-05-28 找到他了',
        '最近总忍不住留意他。

        一开始只是觉得相处起来很舒服，慢慢地，我发现自己好像有点不一样了。

        没事的时候会下意识望向他的方向，听到他说话的声音，心情都会不自觉变好。离我很近的时候，还能闻到他身上淡淡的味道，不像香水，却让人忍不住贪恋。

        大概是真的动心了吧。

        嘻嘻，我在朋友圈找到他的抖音了，以后还能和他续火花，莫名有种偷偷闯进他生活一点点的开心。

        还有他生日发的朋友圈，我估算了一下，他大概二十五岁。也没差多少嘛。

        就是不知道，他会不会介意。',
        '喜欢', 'sunny', 0, '2026-05-28 22:30:05', NULL);

-- 2026-05-29
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-05-29 合拍',
        '今天聊了挺久，从琐事聊到喜好，意外地合拍。

        很多想法都能说到一起去，甚至有些话还没说完，对方就已经明白了。

        这种默契真的很难得。

        结束聊天之后，躺在床上还在回想对话内容。想到某些细节的时候，忍不住偷偷笑出声。

        有点贪心。

        希望这样的时刻，能多一点。',
        '喜欢', 'sunny', 0, '2026-05-29 19:56:38', NULL);

-- 2026-05-30
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-05-30 翻聊天记录',
        '今天没去店里，也没主动发消息。

        空下来的时候，又把聊天记录翻了出来。

        明明只是些很普通的对话，问吃了什么、分享一点生活里的小事、互相吐槽几句，可我却像在翻阅什么珍贵的收藏一样，一条条往上划。

        看到某句玩笑还是会笑，看到某个表情包，又会想起他说这句话时的语气。

        我开始忍不住去猜，他心里会怎么看待我呢？

        我是不是只是一个恰好比较聊得来的弟弟？

        如果有一天我不主动了，我们之间是不是就会这样安静地停下来？

        想到这里，心里忽然空落落的。',
        '喜欢', 'rainy', 0, '2026-05-30 23:12:09', NULL);

-- 2026-05-31
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-05-31 期待又失落',
        '他说他明天可能放假，听到这个消息的时候，我开心坏了。

        甚至偷偷想过，要不要约他出去玩。

        可后来他说已经有安排了。

        那一瞬间，心里还是难免失落。

        至少今天还能和他多聊聊天，也算是一点安慰。

        偏偏明天还要考试。

        见不到他，也没办法找他。

        明明只是一天而已，却莫名觉得时间会变得很漫长。',
        '喜欢', 'cloudy', 0, '2026-05-31 20:08:22', NULL);

-- 2026-06-01
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-01 心疼',
        '他今天的重庆一日游似乎并不理想。

        出去了一整天，没玩上什么，也没吃到什么好吃的，晚上还很晚才回去。

        听他说起这些的时候，我忽然有点心疼。

        难得的假期，却过得这样仓促。

        希望他能早点休息，希望他不要太累。

        喜欢一个人之后，原来真的会因为对方的一点疲惫而跟着难过。',
        '喜欢', 'rainy', 0, '2026-06-01 21:40:15', NULL);

-- 2026-06-02
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-02 替他不值',
        '天呐，他们怎么能这样对姐姐。

        明明是大家一起出去，却一直让他开车。来回八个小时，还把他一个人留在店里。

        光是想想都觉得委屈。

        可他说起这些的时候，却好像没怎么抱怨。

        他总是这样，对别人很好，习惯照顾别人，却很少为自己争取什么。

        我替他感到不值。

        又忍不住更喜欢他一点。',
        '喜欢', 'sunny', 0, '2026-06-02 19:33:57', NULL);

-- 2026-06-03
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-03 失眠',
        '晚上躺在床上，脑海里全是和他相处的画面。

        失眠了大半晚。

        可我一点都不觉得烦躁。

        反而带着一点甜甜的惆怅。

        喜欢这件事，真是让人又欢喜又无奈。

        明明什么都没有发生，却足够让我辗转反侧一整夜。',
        '喜欢', 'cloudy', 0, '2026-06-03 23:47:01', NULL);

-- 2026-06-04
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-04 差点说出口',
        '今天差点把喜欢说出口。

        那一刻，心脏跳得很快。

        我甚至已经开始想象，如果他说他也喜欢我，会是什么样子。

        可最后还是退缩了。

        偏偏他的回答让我捉摸不透，不像接受，也不像明确拒绝。

        我反复琢磨了很久。

        他是不是已经发现了？

        会不会觉得困扰？

        又或者，他只是装作不知道。

        晚上躺在床上，脑海里全是他的样子。

        好像又闻到了他身上淡淡的香味。

        我偷偷幻想，如果我们不止是朋友，会是什么模样。

        可幻想终究只是幻想。

        天亮之后，我还是会把这份心意好好藏起来。',
        '喜欢', 'rainy', 0, '2026-06-04 20:16:44', NULL);

-- 2026-06-05
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-05 想分享',
        '没什么特别的安排，只是心里总挂着一个人。

        看到好玩的东西，第一反应就是想分享给他。

        刷到有趣的视频、听到好听的歌、吃到不错的东西，都会下意识想着：

        “他会不会喜欢？”

        原来喜欢上一个人之后，生活里大大小小的碎片，都想和对方扯上关系。',
        '喜欢', 'sunny', 0, '2026-06-05 22:05:33', NULL);

-- 2026-06-06
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-06 盼消息',
        '今天一整天都在盼着能和他说上话。

        每次拿起手机，都会下意识点开聊天框。

        等到消息弹出的那一刻，嘴角还是会不受控制地扬起来。

        打字的时候总要反复斟酌。

        怕说错话，又想多跟他聊一会儿。

        这种小心翼翼的心情，大概只有自己才懂。',
        '喜欢', 'cloudy', 0, '2026-06-06 21:28:19', NULL);

-- 2026-06-07
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-07 心跳加速',
        '今天在店里，姐姐捧着我的脸和我说话。

        那一瞬间，我感觉自己的心跳快要冲出胸口。

        连呼吸都变得小心翼翼。

        后来我跟他说，暑假可能要回家。

        他明显愣了一下。

        虽然只有短短几秒，可我还是忍不住胡思乱想。

        他是不是有一点舍不得？

        这个念头让我暗自窃喜了很久。

        晚上还和他打了两局游戏。

        很开心。

        特别开心。',
        '喜欢', 'sunny', 0, '2026-06-07 19:52:11', NULL);

-- 2026-06-08
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-08 从来都是我主动',
        '我突然意识到，从来都是我主动找他聊天。

        最后一条消息，总是我发出去的。

        他几乎不会主动来找我。

        可聊天的时候，他又是真诚而开心的。

        我开始怀疑，那些让我心动的瞬间，会不会只是我一个人的错觉。

        我问过AI，让它分析我们的聊天记录。

        它说，姐姐对我有好感。

        可我总觉得不是这样。

        也许她对谁都很好。

        也许只是因为我总是主动。

        换成别人，也会是一样的结果。

        想到这里，心情忽然低落下来。

        可我还是替她找理由。

        她只是太忙了吧。

        喜欢一个人的时候，原来真的很擅长自我安慰。',
        '喜欢', 'rainy', 0, '2026-06-08 23:01:45', NULL);

-- 2026-06-09
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-09 酸酸的',
        '今天无意间看到他主动去和别的男生搭话。

        对方长相很出众。

        他热情得不像话。

        那种主动，是我从未拥有过的。

        说实话，心里挺不是滋味的。

        酸酸的。

        我问AI，它劝我放弃，说要体面退场、及时止损。

        可它只是个人工智能。

        它哪里懂得，喜欢从来不是说停下就能停下的。

        算了。

        就这样吧。

        能偶尔和他说说话，其实也已经很好了。',
        '喜欢', 'rainy', 0, '2026-06-09 20:37:28', NULL);

-- 2026-06-10
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-10 放平心态',
        '刻意让自己放平心态。

        照常聊天、照常相处。

        表面看起来没什么变化，心里却多了一层顾虑。

        不敢把喜欢说出口。

        怕连现在这样轻松自在的关系都维持不住。

        于是把那些翻涌的情绪一点点压回心底。

        装作若无其事。

        装作只是朋友。',
        '喜欢', 'cloudy', 0, '2026-06-10 22:14:03', NULL);

-- 2026-06-11
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-11 嫉妒',
        '今天很不开心。

        为什么他看到帅弟弟的时候，总会那么主动？

        哪怕只是路过，也会忍不住回头多看几眼。

        而我绞尽脑汁找话题，想尽办法延长聊天时间。

        别人什么都不用做，他就会主动靠近。

        而我拼尽全力，也只是勉强维持着现在这样的关系。

        真的很羡慕。

        也很嫉妒。

        原来喜欢一个人以后，自尊心真的会变得很脆弱。

        明明知道他没有错。

        可我还是会因为这些细小的差别，偷偷难过很久。',
        '喜欢', 'rainy', 0, '2026-06-11 21:49:17', NULL);

-- 2026-06-12
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-12 恋爱脑',
        '翻了翻之前的聊天记录。

        看着那些说笑的内容，还是会忍不住嘴角上扬。

        喜欢这件事，真的不受控制。

        一边享受着聊天带来的快乐，一边又会因为他的选择暗自内耗。

        今天刷到一个视频。

        视频里说，恋爱脑的人只是因为从小缺爱。

        我想了很久。

        也许我确实比别人更容易沉溺于被在乎的感觉。

        可我不认为，我喜欢他只是因为他对我好。

        谁会喜欢一个对自己爱搭不理的人呢？

        不论如何，我对他的心意是真的。

        无论最后有没有结果。',
        '喜欢', 'rainy', 0, '2026-06-12 20:05:56', NULL);

-- 2026-06-13
INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-13 今天完工',
        '我第一个真正想部署上线的网站，今天终于快把后端做完了。

        很奇怪。

        这个项目的用户明明只有两个人。

        我和他。

        可我写代码的时候，却格外认真。

        总会忍不住想，如果有一天他真的用到这些功能，会不会觉得：

        “做得还不错。”

        敲代码的间隙，我还是会不自觉地想到他。

        虽然他依旧不会主动来找我。

        偶尔还是会有点失落。

        可只要想到他，我的心情又会莫名变好。

        喜欢真是一件很奇怪的事。

        它会让人失眠、内耗、胡思乱想。

        也会让人想变得更好，想认真完成一些原本会半途而废的事情。

        说到这里，我又有点想见他了。

        只是接下来几天可能会很忙。

        不知道下次见面，又会是什么时候。',
        '喜欢', 'sunny', 0, '2026-06-13 23:25:30', NULL);

INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time, deleted_at)
VALUES (1, '2026-06-14｜想念藏在忙碌里',
        '今天几乎敲了一整天的代码。

        从早到晚，不是在改接口，就是在处理各种细节问题。原本以为忙起来之后，就不会总是胡思乱想，可事实证明，有些人是会偷偷住进你的日常里的。

        网站已经快到部署阶段了。看着一个个功能逐渐完善，心里其实挺有成就感的。毕竟这是我第一个真正想认真做完、想让它上线的项目。更何况，它最初的意义，本来就和他有关。

        今天没有去店里，也没能见到他。

        其实有很多次拿起手机，又默默放下。我知道他最近很忙，不想总去打扰他，也不想让自己的喜欢变成一种负担。

        所以只是偶尔看看聊天框，想着他现在是不是还在工作，有没有按时吃饭，是不是又累得没时间休息。

        明明没聊几句，想念却一点都没有减少。

        以前总觉得，想念应该是轰轰烈烈的，是忍不住一定要告诉对方的。可现在才发现，真正的想念很多时候都很安静。

        是写代码写累了，抬头发呆的时候会想到他。

        是看到有趣的东西，下意识想分享给他。

        是明明知道他很忙，还是会期待手机亮起的那一瞬间。

        有时候也会觉得自己挺没出息的。

        明明还有那么多事情要做，却总能在各种细小的缝隙里想起他。

        可仔细想想，这大概就是喜欢吧。

        不是时时刻刻黏在一起，也不是一定要得到回应，而是即使各自忙碌、各自生活，心里依然会给对方留一个位置。

        等网站部署完成之后，一定要奖励一下自己。

        至于他……

        如果能见上一面，就更好了。',
        '想念', 'sunny', 0, '2026-06-14 21:38:00', NULL);
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
    `media_list`  TEXT     DEFAULT NULL COMMENT 'JSON 数组，每项 {type:"image"|"video", url, width?, height?, duration?}',
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
--     `media_list`  TEXT     DEFAULT NULL COMMENT 'JSON 数组，每项 {type:"image"|"video", url, width?, height?, duration?}',
--     `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     `deleted_at`  DATETIME DEFAULT NULL,
--     PRIMARY KEY (`id`),
--     KEY `idx_pair_time` (`sender_id`, `receiver_id`, `create_time`, `id`),
--     KEY `idx_receiver_time` (`receiver_id`, `create_time`, `id`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- 如果已经是新版 chat_message，仅需补 media_list 列：
-- ALTER TABLE `chat_message`
--     ADD COLUMN `media_list` TEXT DEFAULT NULL
--         COMMENT 'JSON 数组，每项 {type:"image"|"video", url, width?, height?, duration?}'
--         AFTER `content`;

INSERT INTO `chat_message` (`sender_id`, `receiver_id`, `content`, `create_time`) VALUES (1, 2, '早啊', '2026-06-13 10:05:00');
INSERT INTO `chat_message` (`sender_id`, `receiver_id`, `content`, `create_time`) VALUES (2, 1, '早~', '2026-06-13 10:06:00');
INSERT INTO `chat_message` (`sender_id`, `receiver_id`, `content`, `create_time`) VALUES (1, 2, '今天打算把后端的部分弄完', '2026-06-13 10:07:00');
INSERT INTO `chat_message` (`sender_id`, `receiver_id`, `content`, `create_time`) VALUES (2, 1, '加油加油', '2026-06-13 10:08:00');
INSERT INTO `chat_message` (`sender_id`, `receiver_id`, `content`, `create_time`) VALUES (1, 2, '做完之后我们就可以用了', '2026-06-13 10:10:00');
INSERT INTO `chat_message` (`sender_id`, `receiver_id`, `content`, `create_time`) VALUES (2, 1, '好呀 期待', '2026-06-13 10:12:00');
