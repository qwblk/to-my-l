# to-my-l Backend API Reference

> Vue 3 + TypeScript 前端开发完整参考文档  
> Base URL: `http://localhost:8081`

---

## 1. 项目概述

两人私密空间应用（Alice & Bob），功能模块：

| 模块 | 说明 |
|---|---|
| 日记 (Diary) | 写日记，可设私密，日期时间自动注入 |
| 朋友圈 (Moment) | 发动态，可点赞（仅对方）、评论 |
| 留言 (Message) | 给对方留言，已读标识 |
| 聊天室 | WebSocket 实时聊天 + 全局事件推送 |

技术栈：Spring Boot 4.0.7 / MyBatis / Redis / SaToken / WebSocket

两个固定用户：

| ID | username | name | birthday |
|----|----------|------|----------|
| 1  | alice    | Alice | 1995-06-15 |
| 2  | bob      | Bob   | 1998-03-22 |

密码都是 `123456`（BCrypt 加密存储）。

---

## 2. 认证

### 登录

```
POST /user/login
Content-Type: application/json
```

请求：
```json
{ "username": "alice", "password": "123456" }
```

响应：
```json
{ "code": 200, "msg": "Login success", "data": "token-string-here" }
```

### 使用 Token

所有需要认证的接口（除 login / WebSocket / 静态资源外），Header 携带：

```
Authorization: {token}
```

不是 Bearer 格式，直接放 token 字符串。

### 登出

```
POST /user/logout
```

---

## 3. 统一响应格式

SaResult（登录/登出用）：
```json
{ "code": 200, "msg": "success", "data": "..." }
```

Result<T>（业务接口用）：
```json
{ "code": 200, "msg": "success", "data": { } }
```

错误响应：
```json
{ "code": 400, "msg": "Error message" }
{ "code": 401, "msg": "Please login first" }
{ "code": 403, "msg": "No permission" }
{ "code": 500, "msg": "Server busy, try later" }
```

---

## 4. API 接口

### 4.1 用户

**GET /user/me** — 当前用户信息（无 password）

```json
{
  "code": 200, "msg": "success",
  "data": {
    "id": 1, "name": "Alice", "gender": 0,
    "username": "alice", "birthday": "1995-06-15",
    "age": 31,
    "createTime": "2026-06-13 12:00:00",
    "updateTime": "2026-06-13 12:00:00"
  }
}
```

> age 根据 birthday 实时计算，不是数据库字段。

**GET /user/{id}** — 根据 ID 查用户，{id} 为 1 或 2

**GET /user/list** — 所有用户列表，返回 `data: User[]`

### 4.2 日记

**POST /diary** — 创建日记

```json
{
  "title": "标题（必填）",
  "content": "内容（必填）",
  "mood": "happy",
  "weather": "sunny",
  "isPrivate": 0
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| title | YES | 标题 |
| content | YES | 内容 |
| mood | no | happy/sad/excited/calm/anxious/grateful |
| weather | no | sunny/cloudy/rainy/snowy/windy |
| isPrivate | no | 默认 0=公开, 1=仅自己可见 |

日期时间自动注入，不可手动设置。创建后 WebSocket 广播 type="diary"。

**GET /diary/all** — 全部日记，自动过滤掉别人的私密日记。

**GET /diary/mine** — 我的日记。

响应结构：
```json
{
  "id": 1, "userId": 1, "title": "...", "content": "...",
  "mood": "happy", "weather": "sunny", "isPrivate": 0,
  "createTime": "2026-06-13 14:00:00", "userName": "Alice"
}
```

### 4.3 朋友圈

**POST /moment** — 发动态
```json
{ "content": "Beautiful sunset!（必填）", "image": "https://..." }
```

**GET /moment/all** — 全部动态，每个含 likes[] + comments[] + likeCount：
```json
{
  "id": 1, "userId": 1, "userName": "Alice",
  "content": "...", "image": "url",
  "createTime": "...",
  "likes": [
    { "id": 1, "momentId": 1, "userId": 2, "userName": "Bob", "createTime": "..." }
  ],
  "comments": [
    { "id": 1, "momentId": 1, "userId": 2, "userName": "Bob", "content": "Nice!", "createTime": "..." }
  ],
  "likeCount": 1
}
```

**POST /moment/like/{momentId}** — 点赞/取消（toggle）

- 只能给对方点赞，给自己点赞返回 400
- 已点过就取消（返回 `false`），没点过就点上（`true`）
- 操作后 WebSocket 广播 type="like"

**GET /moment/like/{momentId}** — 查点赞列表，返回 `MomentLike[]`

**POST /moment/comment/{momentId}** — 评论
```json
{ "content": "Nice!" }
```

**GET /moment/comment/{momentId}** — 查评论列表，返回 `MomentComment[]`

### 4.4 留言

**POST /message** — 发送留言
```json
{ "receiverId": 2, "content": "See you tomorrow!" }
```

receiverId: 1=Alice 2=Bob。发送后 WebSocket 广播 type="message"。

**GET /message/received** — 我收到的留言

**GET /message/sent** — 我发出的留言

返回结构：
```json
{
  "id": 1, "senderId": 2, "receiverId": 1,
  "senderName": "Bob", "content": "...",
  "isRead": 0, "createTime": "2026-06-13 14:00:00"
}
```

**PUT /message/read/{messageId}** — 标记已读（只有接收方可操作）

标记后 WebSocket 广播 type="read"，通知发送方"对方已读"。

**GET /message/unread-count** — 未读数量，返回 `data: 3`（整数）

---

## 5. WebSocket

### 连接
```
ws://localhost:8081/ws/chat?username=Alice
```

username 取值 Alice 或 Bob，不需要 Auth header。

### 发送（前端→后端）
直接发纯文本字符串（聊天消息）。

### 接收（后端→前端）
```json
{
  "sender": "Alice",
  "content": "Hello!",
  "time": "14:30:01",
  "type": "chat",
  "data": { }
}
```

### type 枚举

| type | 触发时机 | sender | content | data |
|---|---|---|---|---|
| chat | 有人发聊天消息 | 发送者名 | 消息内容 | - |
| online | 有人上线 | 上线者名 | is now online | - |
| offline | 有人下线 | 下线者名 | is now offline | - |
| status | 仅发给新连接者 | SYSTEM | Current online | {online:["Alice"]} |
| diary | 有人写了日记 | SYSTEM | New diary posted | {diaryId, userId} |
| moment | 有人发了动态 | SYSTEM | New moment posted | {momentId, userId} |
| like | 有人点赞/取消 | SYSTEM | Like removed/New like | {momentId, userId, liked} |
| comment | 有人评论了 | SYSTEM | New comment | {momentId, userId} |
| message | 有人发了留言 | SYSTEM | New message | {messageId, senderId, receiverId} |
| read | 对方读了你的留言 | SYSTEM | Message read | {messageId, senderId} |

### 在线状态处理

- `type: "online"` — 标记对方在线
- `type: "offline"` — 标记对方离线
- `type: "status"` — 仅新连接者收到，`data.online` 是已在线用户名数组。空数组表示对方不在线

---

## 6. TypeScript 类型

```typescript
// ====== 通用 ======
interface Result<T> { code: number; msg: string; data: T }

// ====== 用户 ======
interface LoginRequest { username: string; password: string }

interface User {
  id: number; name: string; gender: number; username: string
  birthday: string; age: number
  createTime: string; updateTime: string
}

// ====== 日记 ======
interface DiaryCreateRequest {
  title: string; content: string
  mood?: string; weather?: string; isPrivate?: number
}

interface Diary {
  id: number; userId: number
  title: string; content: string
  mood: string | null; weather: string | null
  isPrivate: number; createTime: string; userName: string
}

// ====== 朋友圈 ======
interface MomentCreateRequest { content: string; image?: string }

interface Moment {
  id: number; userId: number; userName: string
  content: string; image: string | null
  createTime: string
  likes: MomentLike[]; comments: MomentComment[]; likeCount: number
}

interface MomentLike {
  id: number; momentId: number; userId: number
  userName: string; createTime: string
}

interface MomentCommentRequest { content: string }

interface MomentComment {
  id: number; momentId: number; userId: number
  content: string; userName: string; createTime: string
}

// ====== 留言 ======
interface MessageSendRequest { receiverId: number; content: string }

interface Message {
  id: number; senderId: number; receiverId: number
  content: string; isRead: number; senderName: string; createTime: string
}

// ====== WebSocket ======
type WsType = 'chat' | 'online' | 'offline' | 'status' | 'diary' |
              'moment' | 'like' | 'comment' | 'message' | 'read' | 'system'

interface WsMessage {
  sender: string; content: string; time: string
  type: WsType; data?: Record<string, any>
}
```

---

## 7. Vue 3 项目结构建议

```
src/
├── api/
│   ├── client.ts          # axios 实例 + 拦截器
│   ├── user.ts            # 用户 API
│   ├── diary.ts           # 日记 API
│   ├── moment.ts          # 朋友圈 API
│   └── message.ts         # 留言 API
├── ws/
│   └── useWebSocket.ts    # composable
├── stores/                # Pinia
│   ├── auth.ts            # 登录状态
│   ├── user.ts            # 用户信息
│   └── chat.ts            # 聊天 + 在线状态
├── types/
│   └── index.ts           # 所有 TS 类型
├── views/
│   ├── LoginView.vue
│   ├── DiaryView.vue
│   ├── MomentView.vue
│   ├── MessageView.vue
│   └── ChatView.vue
├── components/
│   ├── AppHeader.vue      # 顶部导航 + 对方在线状态
│   └── NotificationToast.vue
├── router/index.ts
├── App.vue
└── main.ts
```

---

## 8. 关键实现代码

### axios 实例

```typescript
// api/client.ts
import axios from 'axios'

const client = axios.create({ baseURL: 'http://localhost:8081' })

client.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = token
  return config
})

client.interceptors.response.use(
  res => res.data,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(err.response?.data || err)
  }
)

export default client
```

### WebSocket Composable

```typescript
// ws/useWebSocket.ts
import { ref, onUnmounted } from 'vue'
import type { WsMessage } from '@/types'

export function useWebSocket(username: string) {
  const messages = ref<WsMessage[]>([])
  const partnerOnline = ref(false)
  let ws: WebSocket | null = null

  function connect() {
    const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
    ws = new WebSocket(`${proto}//${location.host}/ws/chat?username=${username}`)

    ws.onmessage = (e) => {
      const msg: WsMessage = JSON.parse(e.data)
      messages.value.push(msg)

      if (msg.type === 'online' && msg.sender !== username) partnerOnline.value = true
      if (msg.type === 'offline' && msg.sender !== username) partnerOnline.value = false
      if (msg.type === 'status') {
        const list = msg.data?.online?.filter((u: string) => u)
        partnerOnline.value = list && list.length > 0
      }

      if (msg.sender !== username && document.hidden && 'Notification' in window) {
        new Notification(msg.type, { body: msg.content })
      }
    }
  }

  function send(text: string) { ws?.send(text) }
  function disconnect() { ws?.close() }
  onUnmounted(disconnect)

  return { messages, partnerOnline, connect, send, disconnect }
}
```

---

## 9. 注意事项

1. Token 存 localStorage，axios 拦截器自动附加
2. WebSocket username 参数用登录后的 username，不是昵称
3. 日期格式 `yyyy-MM-dd HH:mm:ss`，前端 `new Date()` 可解析
4. age 字段后端计算，前端不用自己算
5. `/diary/all` 已自动过滤他人私密日记
6. 点赞只能用对方的动态、标记已读只能是接收方——后端已校验
7. CORS 已全开，开发环境无需代理
8. Redis 缓存列表数据，写操作后自动失效。如果数据延迟等几秒刷新

---

## 10. 辅助文件

项目目录下的 `src/main/resources/static/openapi.json` 可导入 Apifox 一键生成全部接口。
