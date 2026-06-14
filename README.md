# to-my-l

> 两人专属私密空间 —— 日记 · 朋友圈 · 留言 · 实时聊天

---

## 技术栈

| 层 | 技术 |
|---|---|
| 框架 | Spring Boot 4.0.7 |
| ORM | MyBatis + 注解 SQL |
| 缓存 | Redis（Spring Cache，含雪崩/穿透/击穿防护） |
| 认证 | Sa-Token（Token + 路由拦截） |
| 实时通信 | WebSocket（聊天 + 全局事件推送） |
| 密码加密 | BCrypt（spring-security-crypto） |
| 数据库 | MySQL 8 |
| 语言 | Java 17 |
| 构建 | Maven |

---

## 功能模块

| 模块 | 说明 | 路由前缀 |
|---|---|---|
| 用户 | 2 个固定用户，登录/改密/改信息，首次登录欢迎语 | `/user` |
| 日记 | 写日记，标题/内容/心情/天气/私密，日期时间自动注入 | `/diary` |
| 朋友圈 | 发动态（支持图片），对方可点赞/评论，不可给自己点赞 | `/moment` |
| 留言 | 发送留言，已读标识，未读计数 | `/message` |
| 聊天室 | WebSocket 实时聊天，消息持久化，历史记录，在线状态 | `/ws/chat` |
| 文件上传 | 图片/视频上传至本地磁盘，返回可访问 URL | `/upload` |

### 固定用户

| ID | 用户名 | 姓名 | 生日 | 密码 |
|---|-----|---|---|---|
| 1 | 王水群 | Alice | 1995-06-15 | 123456 |
| 2 | 潘佩雪 | Bob | 1998-03-22 | 123456 |

> 不支持注册。密码 BCrypt 加密存储。

---

## 快速启动

### 1. 环境准备

- JDK 17+
- MySQL 8+（已启动，端口 3306）
- Redis（已启动，端口 6379）

### 2. 初始化数据库

执行 `src/main/resources/sql/init.sql`：

```bash
mysql -u root -p < src/main/resources/sql/init.sql
```

会在 `to_my_l` 库中创建所有表并插入 2 个初始用户。

### 3. 修改配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://你的MySQL地址:3306/to_my_l
    username: 你的用户名
    password: 你的密码
  data:
    redis:
      host: 你的Redis地址
      port: 6379
      password: 你的Redis密码
```

### 4. 启动

```bash
./mvnw spring-boot:run
```

服务启动在 `http://localhost:8081`。

### 5. 验证

```bash
# 登录
curl -X POST http://localhost:8081/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"123456"}'

# 返回 token 后，用 token 访问其他接口
curl http://localhost:8081/user/me -H "Authorization: <token>"
```

---

## 项目结构

```
src/main/java/com/panpeixue/myl/
├── ToMyLApplication.java           # 启动类
├── common/
│   ├── Result.java                 # 统一返回体
│   └── GlobalExceptionHandler.java # 全局异常处理（400/401/403/405/500）
├── config/
│   ├── CorsConfig.java             # 跨域
│   ├── RedisConfig.java            # Redis 序列化 + 缓存防护
│   ├── SaTokenConfig.java          # 登录拦截 + 路由放行
│   ├── WebMvcConfig.java           # 静态资源映射（上传文件访问）
│   └── WebSocketConfig.java        # WebSocket 端点注册
├── controller/
│   ├── UserController.java         # /user/*
│   ├── DiaryController.java        # /diary/*
│   ├── MomentController.java       # /moment/*
│   ├── MessageController.java      # /message/*
│   └── FileUploadController.java   # /upload
├── mapper/                         # MyBatis 接口（注解 SQL）
├── model/
│   ├── pojo/                       # 数据库实体
│   ├── dto/                        # 前端请求体（XxxRequest）
│   └── vo/                         # 前端响应体（XxxVO）
├── service/                        # 业务接口 + impl 实现
└── websocket/
    ├── ChatWebSocketHandler.java   # 聊天处理 + 事件广播
    └── WebSocketSessionManager.java# Session 管理 + 在线状态
```

---

## API 概览

全部 24 个接口详见 [`API.md`](API.md)，或导入 [`openapi.json`](src/main/resources/static/openapi.json) 到 Apifox/Postman。

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| POST | `/user/login` | 登录（返回 token + firstLogin） | 否 |
| GET | `/user/me` | 当前用户信息 | 是 |
| PUT | `/user/password` | 改密码 | 是 |
| PUT | `/user/info` | 改个人信息 | 是 |
| GET | `/user/list` | 用户列表 | 是 |
| POST | `/user/logout` | 登出 | 是 |
| POST | `/diary` | 写日记 | 是 |
| GET | `/diary/all` | 全部日记（过滤私密） | 是 |
| GET | `/diary/mine` | 我的日记 | 是 |
| POST | `/moment` | 发动态 | 是 |
| GET | `/moment/all` | 全部动态（含点赞/评论） | 是 |
| POST | `/moment/like/{id}` | 点赞/取消 | 是 |
| POST | `/moment/comment/{id}` | 评论 | 是 |
| POST | `/message` | 发送留言 | 是 |
| GET | `/message/received` | 收到的留言 | 是 |
| GET | `/message/sent` | 发出的留言 | 是 |
| PUT | `/message/read/{id}` | 标为已读 | 是 |
| GET | `/message/unread-count` | 未读计数 | 是 |
| POST | `/upload` | 上传图片/视频 | 否 |
| WS | `/ws/chat` | WebSocket 聊天 | 否 |

### 统一响应格式

成功：
```json
{"code": 200, "msg": "success", "data": { ... }}
```

失败：
```json
{"code": 400, "msg": "Error message"}
```

认证失败返回 `{"code": 401, "msg": "Please login first"}`。

---

## WebSocket 事件

连接：`ws://localhost:8081/ws/chat?username=Alice`

服务端推送 JSON：
```json
{"sender":"Alice","content":"Hello!","time":"14:30:01","type":"chat","data":{}}
```

| type | 触发时机 |
|---|---|
| `chat` | 收到聊天消息 |
| `online` | 对方上线 |
| `offline` | 对方下线 |
| `status` | 连接时同步当前在线列表 |
| `history` | 连接时推送最近 100 条聊天记录 |
| `diary` | 有人写了日记 |
| `moment` | 有人发了动态 |
| `like` | 有人点赞/取消 |
| `comment` | 有人评论 |
| `message` | 收到新留言 |
| `read` | 对方已读留言 |

---

## 缓存策略

| 数据 | 缓存名 | TTL | 防护 |
|---|---|---|---|
| 用户详情 | `userCache` | 60min | 击穿（sync=true） |
| 日记列表 | `diaryList` | 30min | 击穿 |
| 动态列表 | `momentList` | 10min | 击穿 |
| 留言列表 | `messageList` | 5min | 击穿 |

- **雪崩**：所有 TTL 叠加 0~20% 随机偏移
- **穿透**：`nullCache` 分区（2min），可缓存空结果
- **击穿**：`@Cacheable(sync=true)`，Redis 分布式锁

写操作后对应缓存自动失效（`@CacheEvict`）。

---

## 部署

```bash
# 打包
./mvnw clean package -DskipTests

# 上传到服务器后运行
java -jar target/myl-0.0.1-SNAPSHOT.jar
```

上传文件的存储路径通过 `application.yml` 中的 `app.upload-path` 配置，默认为 `./uploads`。部署到云服务器后建议改为绝对路径（如 `/data/uploads`）并加入备份策略。

---

## 开发相关

- [API.md](API.md) —— 完整 API 文档 + Vue 3 前端开发指南
- [openapi.json](src/main/resources/static/openapi.json) —— Apifox 可导入的 OpenAPI 3.0 文件
- [chat.html](src/main/resources/static/chat.html) —— 原生聊天室测试页面（在线状态 + 桌面通知）
- [init.sql](src/main/resources/sql/init.sql) —— 数据库初始化脚本
