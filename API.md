# to-my-l API 文档

> 后端：Spring Boot 4.0.7 + MyBatis + Redis + SaToken
> base URL：`http://<host>:8081`
> 鉴权：除标注「免鉴权」外，所有接口需要 `Authorization: <token>` 头（token 来自 `/user/login`）
> 错误码：HTTP 始终 200，业务错误码在响应体 `code` 字段（401 未登录 / 403 无权限 / 400 参数错误 / 413 文件过大 / 415 类型不支持 / 500 服务器错误）

---

## 通用响应格式

```jsonc
{
  "code": 200,         // 200 成功；其它见上
  "msg":  "success",
  "data": <object|array|null>
}
```

时间字段统一为 `yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai`。

---

## 认证 / Token 生命周期

后端用 SaToken，token 通过 `Authorization` 请求头携带（值就是 `/user/login` 返回的 `data.token`，**不需要加 `Bearer` 前缀**）。

token 的过期机制是双闸门：

| 名称 | 当前值 | 含义 |
|---|---|---|
| 绝对过期 `timeout` | 30 天 | 从 token **创建那一刻**开始倒计时，**不会被任何活动重置**。到点必须重新登录 —— 安全防线 |
| 活跃过期 `active-timeout` | 7 天 | 每次该 token 命中受保护接口都会**自动重置**这个倒计时（开启了滑动续期 `auto-renew=true`）。换句话说只要至少每 7 天有一次请求，就一直续命 |

实际效果：

- 用户日常使用 → 30 秒一次心跳 + 各种接口操作 → 活跃过期被持续重置 → 不会过期
- 锁屏 / 周末 / 出差几天没动 → 仍在 7 天内 → 下次请求自动续期，无感
- 离开超过 7 天 → 活跃过期触发 → 下次请求返 `401 Please login first`，前端按"登录过期"处理
- 累计使用超过 30 天 → 即使一直活跃，绝对过期触发 → 强制 `401`

> 滑动续期由 SaToken 内置完成，**不会签发新 token**，前端不需要换 token 也无需任何处理。

### 错误码区分

| code | 触发场景 |
|---|---|
| 401 `Please login first` | 没带 token、token 无效、活跃过期、绝对过期 |
| 4012 `Logged in elsewhere` | 同账号在新设备登录把当前 token 顶下线（与 `is-concurrent: false` 配合） |

前端拦截器建议：

- `401` → 清 token，跳登录页
- `4012` → 清 token，弹"账号已在其他设备登录，请重新登录"，跳登录页

### WebSocket

当前 `/ws/chat` 通过 query 参数 `?token=xxx` 建立连接，后端会在握手时用 SaToken 校验 token 并解析当前用户。

- token 有效：连接建立，后端从 token 绑定用户身份
- token 无效 / 过期：后端发 `type=error`，随后 close code `4003`
- 同账号新连接：旧连接收到 `type=kicked`，随后 close code `4001`

---

## 用户 `/user`

### `POST /user/login` —— 登录（免鉴权）

请求：
```json
{ "username": "wangshuiqun", "password": "123456" }
```
响应 data：
```jsonc
{
  "token": "abc...64...",
  "partnerOnline": true,
  "firstLogin": false,
  "greeting": "Welcome..."   // firstLogin=true 才有
}
```

> ⚠️ 登录**不会**更新 `last_seen_at`。前端先调 `GET /user/last-seen` 拿上次离开时间，再调 `PUT /user/heartbeat` 推到 NOW。

> 🔐 **单点登录策略**：同一账号在新设备登录会让旧设备的 token 立即失效。
> - 旧 token 后续调任何受保护接口会返回 `code: 4012, msg: "Logged in elsewhere"`
> - 旧 WebSocket 连接会先收到一帧 `type: "kicked"`，然后被 close（close code = `4001`）
> - 新设备照常工作；登录响应不变

### `GET /user/me`

响应 data 是当前登录用户：
```json
{
  "id": 1, "name": "王水群", "gender": 1, "username": "wangshuiqun",
  "birthday": "2006-07-29",
  "bio": "...",                         // deprecated；内部仍落 user.bio 列
  "profileText": "这里是一段写给对方的话", // 新字段名，值和 bio 相同
  "isFirstLogin": 0,
  "lastSeenAt": "2026-06-13 23:45:12",
  "createTime": "2026-05-01 10:00:00",
  "updateTime": "2026-06-13 23:45:12"
}
```

### `PUT /user/me`

修改当前登录用户自己的资料。只能改自己，后端从 token 取 userId，不接受 body 里的 id。

请求：
```json
{
  "name": "王水群",
  "gender": 1,
  "birthday": "2002-01-01",
  "profileText": "这里是一段很长的话..."
}
```

规则：
- `name` 必填，trim 后不能为空
- `gender` 可为空；不为空时只能是 `0` 或 `1`
- `birthday` 格式 `yyyy-MM-dd`
- `profileText` 可为空字符串，最大长度 10000
- 成功返回更新后的 User，并清理当前用户 `userCache`

### `GET /user/{id}`、`GET /user/list`

返回单个 / 全部用户（同上结构，密码字段不返回）。双方都可看到对方 `profileText`。

### `PUT /user/password`

```json
{ "oldPassword": "...", "newPassword": "..." }   // newPassword ≥ 6 位
```

### `PUT /user/info`

旧接口别名，继续保留兼容；新前端建议使用 `PUT /user/me`。

```json
{ "name": "...", "gender": 0|1, "birthday": "yyyy-MM-dd", "profileText": "..." }
```

### `PUT /user/heartbeat`

更新当前用户 `last_seen_at = NOW()`。**幂等**，连调多次只更新时间。
前端建议每 30s（visibilityState=visible）+ beforeunload 各调一次。
data 为 null。

### `GET /user/last-seen`

```jsonc
{ "code": 200, "data": { "lastSeenAt": "2026-06-12 23:30:00" } }
// 从未活跃过：lastSeenAt = null
```

### `GET /user/online-status`

返回当前在线用户名集合（`Set<String>`）。

### `POST /user/logout`

注销当前 token。

---

## 文件上传 `/upload`

### `POST /upload`

- 鉴权：登录用户（未登录 → 401）
- 请求：`multipart/form-data`，字段名 `file`
- 接受 mime：
  - 图片：`image/jpeg`、`image/png`、`image/webp`、`image/gif`
  - 视频：`video/mp4`、`video/quicktime`（iOS .mov）
- 大小限制：图片 ≤ 10MB（413）、视频 ≤ 50MB（413）
- 其它类型：415

响应 data：
```jsonc
{
  "url": "/static/uploads/2026/06/<uuid>.jpg",
  "type": "image",      // 或 "video"
  "width": 1024,        // 仅图片，且 ImageIO 能解码时
  "height": 768
  // duration 字段不返回 —— 前端用 <video>.loadedmetadata 拿，零依赖且更准
}
```

错误样例：
```jsonc
{ "code": 415, "msg": "Unsupported file type: text/plain. Allowed: ..." }
{ "code": 413, "msg": "Image too large: 12345678 bytes, limit 10485760" }
{ "code": 401, "msg": "Please login first" }
```

### 静态资源访问

- `GET /static/uploads/2026/06/<uuid>.jpg` —— 当前规范，前端用这个拼 URL
- `GET /uploads/<...>` —— 历史 URL，老 `moment.image` 数据兜底

两条都映射到磁盘目录 `app.upload-path`（容器里默认 `/app/uploads`，本地默认 `./uploads`）。

---

## 朋友圈 `/moment`

### `POST /moment`

```json
{
  "content": "今天天气真好",
  "mediaList": [
    { "type": "image", "url": "/static/uploads/2026/06/a.jpg", "width": 1024, "height": 768 },
    { "type": "video", "url": "/static/uploads/2026/06/b.mp4", "duration": 12.5 }
  ]
}
```

校验：
- `content` 必填，trim 后长度 ≥ 1（空 → 400）
- `mediaList` 可选，最多 9 个（朋友圈惯例）
- 每项 `type ∈ {"image","video"}`、`url` 必须以 `/static/uploads/` 开头（防外站 URL → 400）

响应 data：返回创建后的 Moment 对象（结构同 `GET /moment/all` 列表项）。

> 旧字段 `image` 已 deprecated，新发动态请只传 `mediaList`，后端不会写 `image` 列。

### `GET /moment/all`

返回所有未软删除动态（`moment.deleted_at IS NULL`），按 `createTime DESC`：

```jsonc
[
  {
    "id": 4,
    "userId": 1,
    "userName": "王水群",
    "content": "...",
    "image": null,                    // deprecated；老数据可能有值
    "mediaList": [                    // 新字段：始终是数组（可能为空）
      { "type": "image", "url": "/static/uploads/...", "width": 1024, "height": 768 },
      { "type": "video", "url": "/static/uploads/..." }
    ],
    "createTime": "2026-06-13 22:00:00",
    "likes":    [{ "id": 5, "momentId": 4, "userId": 2, "userName": "潘佩雪", "createTime": "..." }],
    "comments": [{ "id": 8, "momentId": 4, "userId": 2, "userName": "潘佩雪", "content": "...", "createTime": "..." }],
    "likeCount": 1
  }
]
```

兼容性：
- 老数据 `image` 有值、`media_list` 列为 null → `mediaList = [{type:"image", url:image}]`
- 新数据 `media_list` 列存 JSON 字符串 → 反序列化为对象数组下发
- DB 里 `media_list` JSON 损坏时降级到 `image` 字段，不抛错（日志告警）

### `DELETE /moment/{momentId}`

软删除自己的瞬间。

权限 / 返回：
- 只能删自己发的瞬间；删别人的 → `code=403, msg="No permission"`
- 不存在或已删除 → `code=404, msg="Moment not found"`
- 成功 → `{ "code": 200, "msg": "success", "data": null }`

行为：
- `moment.deleted_at = NOW()`
- 关联 `moment_like.deleted_at = NOW()`、`moment_comment.deleted_at = NOW()`（不物理删除）
- 不删除磁盘上的图片/视频文件
- 广播 WS：
```json
{
  "sender": "SYSTEM",
  "content": "Moment deleted",
  "type": "moment_delete",
  "data": { "momentId": 123, "userId": 1 }
}
```

### `POST /moment/like/{momentId}`

切换点赞（已点过则取消）。**不能给自己点赞**（400）。Moment 不存在或已软删 → `404 Moment not found`。返回 `data: true|false`（true=新增点赞，false=取消）。

### `GET /moment/like/{momentId}`

返回该动态的未软删点赞列表（`MomentLike[]`）。Moment 不存在或已软删 → `404 Moment not found`。

### `POST /moment/comment/{momentId}`

```json
{ "content": "..." }   // 不能空
```

### `GET /moment/comment/{momentId}`

返回该动态的未软删评论列表（`MomentComment[]`）。Moment 不存在或已软删 → `404 Moment not found`。

---

## 日记 `/diary`

### `POST /diary`

```json
{
  "title": "...", "content": "...",
  "mood": "happy", "weather": "sunny", "isPrivate": 0
}
```

### `DELETE /diary/{diaryId}`

软删除自己的日记。

权限 / 返回：
- 只能删自己写的日记；删别人的 → `code=403, msg="No permission"`
- 不存在或已删除 → `code=404, msg="Diary not found"`
- 成功 → `{ "code": 200, "msg": "success", "data": null }`

行为：
- `diary.deleted_at = NOW()`
- 不做级联（diary 无 like/comment 子表）
- 广播 WS：
```json
{
  "sender": "SYSTEM",
  "content": "Diary deleted",
  "type": "diary_delete",
  "data": { "diaryId": 123, "userId": 1 }
}
```

### `PUT /diary/{diaryId}/privacy`

只改 `is_private` 一个字段，不会覆盖正文。

请求：
```json
{ "isPrivate": 0 }   // 或 1
```

权限 / 返回：
- 只能改自己写的日记；改别人的 → `code=403, msg="No permission"`
- 不存在或已软删 → `code=404, msg="Diary not found"`
- `isPrivate` 不是 0 或 1 → `code=400`
- 成功 → `{ "code": 200, "msg": "success", "data": Diary }`

行为：
- `diary.is_private = 入参`
- 清缓存 `diaryList`
- 广播 WS：
```json
{
  "sender": "SYSTEM",
  "content": "Diary updated",
  "type": "diary",
  "data": { "diaryId": 123, "userId": 1, "isPrivate": 1 }
}
```

### `GET /diary/all`、`GET /diary/mine`

返回未软删除日记列表（`diary.deleted_at IS NULL`），每条带 `createTime`（格式 `yyyy-MM-dd HH:mm:ss`）。

> `Diary.createTime` 与 `User.lastSeenAt` 字符串格式严格一致 —— 前端可以直接做 `diary.createTime > user.lastSeenAt` 字典序比较，判定是否「离线期间产生」。

### `GET /diary/days`

按天分页返回日记分组，用于时间轴懒加载。

参数：
- `scope`: `all | mine`，默认 `all`
- `cursorDate`: `yyyy-MM-dd`，可选；传了表示只返回 `date < cursorDate` 的日期，避免重复上一页最后一天
- `size`: 默认 10，最大 30（超过会 clamp 到 30）

权限：
- `scope=mine`：只返回当前用户自己的日记
- `scope=all`：沿用 `/diary/all` 可见性：自己的全部 + 对方非私密
- 始终过滤 `deleted_at IS NULL`

响应：
```jsonc
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [
      {
        "date": "2026-06-14",
        "weekday": "星期日",
        "entries": [ /* Diary[], 当天 create_time ASC, id ASC */ ]
      }
    ],
    "nextCursorDate": "2026-06-03",
    "hasMore": true
  }
}
```

### `GET /diary/day`

按指定日期查询日记。

参数：
- `date`: `yyyy-MM-dd`，必填
- `scope`: `all | mine`，默认 `all`

响应：
```jsonc
{
  "code": 200,
  "msg": "success",
  "data": {
    "date": "2026-06-14",
    "weekday": "星期日",
    "entries": [ /* Diary[], create_time ASC, id ASC */ ]
  }
}
```

当天无数据时 `entries=[]`，不返回 404。

---

## 留言 `/message`

### `POST /message`

```json
{ "receiverId": 2, "content": "..." }
```

### `GET /message/received`、`GET /message/sent`

旧接口：一次性返回收件箱 / 发件箱列表，继续保留兼容。

### `GET /message/received/page`

漂流瓶收件箱分页。

参数：
- `cursor`: `yyyy-MM-dd HH:mm:ss`，可选；上一页返回的 `nextCursor`
- `cursorId`: number，可选；上一页返回的 `nextCursorId`
- `size`: 默认 20，最大 50（超过会 clamp 到 50）

> 推荐前端同时传 `cursor + cursorId`，这样同一秒内多条消息不会漏。只传旧版 `cursor` 也兼容，但同秒边界仍可能跳过剩余消息。

规则：
- 只返回 `receiver_id = 当前用户`
- 排序：`create_time DESC, id DESC`

响应：
```jsonc
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [ /* Message[] */ ],
    "nextCursor": "2026-06-12 18:00:00",
    "nextCursorId": 123,
    "hasMore": true
  }
}
```

### `GET /message/sent/page`

漂流瓶发件箱分页。参数/响应同上，规则改为只返回 `sender_id = 当前用户`。

### `PUT /message/read/{id}`、`GET /message/unread-count`

标记已读、未读数。

---

## 聊天 `/chat` + `/ws/chat`

### `GET /chat/history`

聊天历史分页接口（登录用户）。

参数：
- `cursor`: `yyyy-MM-dd HH:mm:ss`，可选；上一页返回的 `nextCursor`
- `cursorId`: number，可选；上一页返回的 `nextCursorId`
- `size`: 默认 30，最大 50（超过会 clamp 到 50）

> 推荐前端同时传 `cursor + cursorId`，查询条件会使用 `(create_time < cursor OR (create_time = cursor AND id < cursorId))`，避免同一秒内多条消息翻页时丢数据。只传旧版 `cursor` 也兼容。

规则：
- 当前用户只能查询自己和对方之间的消息
- `deleted_at IS NULL`
- 排序：`create_time DESC, id DESC`
- 后端多查 `size + 1` 判断 `hasMore`
- `nextCursor` = 本页最后一条消息的 `createTime`
- `nextCursorId` = 本页最后一条消息的 `id`
- 前端收到后可按 `createTime ASC` 显示

响应：
```jsonc
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [
      {
        "id": 123,
        "senderId": 1,
        "receiverId": 2,
        "senderName": "王水群",
        "content": "你好",
        "createTime": "2026-06-14 22:10:03",
        "mediaList": [
          { "type": "image", "url": "/static/uploads/2026/06/a.jpg", "width": 1024, "height": 768 }
        ]
      }
    ],
    "nextCursor": "2026-06-12 18:00:00",
    "nextCursorId": 123,
    "hasMore": true
  }
}
```

### WebSocket `/ws/chat`

WebSocket 端点：`ws://<host>:8081/ws/chat?token=<token>`。

> 现在 WS 握手必须带有效 SaToken，不再接受 `?username=` 作为身份来源。后端会从 token 解析当前用户，避免伪造 username 读取聊天历史或踢掉真实用户。
>
> 浏览器 WebSocket 无法自定义 Header，前端用 query 参数即可：
> ```ts
> const ws = new WebSocket(`${WS_BASE}/ws/chat?token=${encodeURIComponent(token)}`)
> ```
> 非浏览器客户端也可以用 Header `Authorization: <token>`。token 过期/无效时后端会先发 `type=error`，随后 close code `4003`。

前端发送（推荐 JSON）：
```jsonc
{ "type": "chat", "content": "你好" }
{ "type": "chat", "content": "", "mediaList": [
  { "type": "image", "url": "/static/uploads/2026/06/a.jpg", "width": 1024, "height": 768 },
  { "type": "video", "url": "/static/uploads/2026/06/b.mp4", "duration": 12.5 }
] }
{ "type": "heart" }
```

兼容旧前端：
- 纯文本仍按 `chat` 处理
- `__TML_HEART__` 识别为 `heart`，不保存、不展示成聊天文本

服务端广播普通聊天（带或不带 mediaList）：
```jsonc
{
  "sender": "王水群",
  "content": "你好",
  "time": "22:10:03",
  "type": "chat",
  "data": {
    "id": 123,
    "senderId": 1,
    "receiverId": 2,
    "createTime": "2026-06-14 22:10:03",
    "mediaList": [
      { "type": "image", "url": "/static/uploads/2026/06/a.jpg", "width": 1024, "height": 768 }
    ]
  }
}
```

> 没有图片/视频时 `data.mediaList` 为 `[]`，不会缺字段。

服务端广播心动：
```jsonc
{
  "sender": "王水群",
  "content": "heart",
  "time": "22:10:03",
  "type": "heart",
  "data": {
    "senderId": 1,
    "receiverId": 2
  }
}
```

规则：
- `chat`：`content` trim 后允许空，但**必须 content 或 mediaList 至少有一个**；`content` 长度 ≤ 500
- `mediaList`：可选，最多 9 个；每项 `type ∈ {"image","video"}`、`url` 必须以 `/static/uploads/` 开头（与 `/moment` 保持同一规则）
- 校验失败时不会广播给对方，只回发一条 `type: "error"` 给发送方
- `heart`：不保存 DB，只广播给自己和对方；对方离线时不补发
- 新连接后会推一条 `history` 事件，`data.messages` 是最近 30 条聊天历史（同样含 `mediaList` 字段）

事件类型 (`type` 字段)：
- `chat` —— 普通聊天消息
- `heart` —— 心动事件
- `history` —— 连接建立后下发的历史
- `online` / `offline` —— 上下线广播
- `status` —— 当前在线列表
- `kicked` —— 被同账号新登录顶下线（详见下面）
- `moment` —— 新动态广播
- `moment_delete` —— 动态软删除广播（`data: { momentId, userId }`）
- `diary_delete` —— 日记软删除广播（`data: { diaryId, userId }`）
- `like` / `comment` —— 互动广播

#### `kicked` 事件

同账号在新设备建立 WebSocket 连接时，旧连接会先收到这一帧、紧接着被 `close(4001)`：

```jsonc
{
  "sender": "SYSTEM",
  "content": "Logged in elsewhere",
  "time": "22:10:03",
  "type": "kicked",
  "data": { "at": "2026-06-14 22:10:03" }
}
```

前端建议：
- 收到 `type: "kicked"` 立即弹"已在其他设备登录"提示
- 监听 `onclose`，如果 `event.code === 4001` 也按"被踢下线"处理（防止 `kicked` 帧因网络延迟未到达）
- 不要自动重连（重连会触发新一轮踢自己），引导用户重新登录

> 后端用 `ConcurrentWebSocketSessionDecorator` 包了每个 session，多用户并发上线/广播不会再触发 `TEXT_PARTIAL_WRITING` 异常。

---

## 错误码速查

| code | 含义 | 触发场景 |
|---|---|---|
| 200 | 成功 | 正常响应 |
| 400 | 参数错误 | 空 content、外站 URL、超长 mediaList、JSON 解析失败等 |
| 401 | 未登录 | 没带 / token 失效；SaServletFilter 拦下 |
| 403 | 无权限 | 当前用例几乎用不到 |
| 405 | 方法不支持 | 用错 HTTP 方法 |
| 413 | 文件过大 | 图片 > 10MB、视频 > 50MB |
| 415 | 文件类型不支持 | 上传白名单外的 mime |
| 4012 | 被顶下线 | 同账号在另一设备新登录，旧 token 调接口时返回 |
| 500 | 服务器错误 | DB / 文件系统异常 |

---

## 数据库迁移备忘

`user` 表加 `last_seen_at`：
```sql
ALTER TABLE `user`
  ADD COLUMN `last_seen_at` DATETIME DEFAULT NULL
    COMMENT '用户最后一次活跃时间，用于离线追赶'
    AFTER `is_first_login`;
```

`moment` 表加 `media_list`：
```sql
ALTER TABLE `moment`
  ADD COLUMN `media_list` TEXT DEFAULT NULL
    COMMENT 'JSON 数组，每项 {type:"image"|"video", url, width?, height?, duration?}'
    AFTER `image`;
```

`image` 列**保留不删**，老数据走 `image`，新数据走 `media_list`，前端对外只看 `mediaList` 字段。

软删除列：
```sql
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
```

聊天表新结构（如果旧表是 sender_name 版，建议备份后重建）：
```sql
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

-- 已经是新版只缺 media_list 时：
ALTER TABLE `chat_message`
  ADD COLUMN `media_list` TEXT DEFAULT NULL
    COMMENT 'JSON 数组，每项 {type:"image"|"video", url, width?, height?, duration?}'
    AFTER `content`;
```
