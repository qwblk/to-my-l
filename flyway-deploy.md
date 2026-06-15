# Flyway 接入与上线步骤

## 概念

- 项目用 **Flyway** 管理数据库迁移
- 迁移脚本放在 [src/main/resources/db/migration/](src/main/resources/db/migration/)
- 命名 `V<版本号>__<描述>.sql`，版本号必须递增
- 应用启动时 Flyway 自动比对 `flyway_schema_history` 表，缺哪步跑哪步
- 默认 **关闭**（`FLYWAY_ENABLED=false`）。第一次接入时手动打开，确认无误后保持开启

## 当前已有的迁移

| 版本 | 文件 | 内容 |
|---|---|---|
| V1 | `V1__init_baseline.sql` | 项目最初部署版本的全部表 + 种子用户（用作 baseline） |
| V2 | `V2__user_last_seen_at.sql` | `user.last_seen_at` |
| V3 | `V3__moment_media_list.sql` | `moment.media_list` |
| V4 | `V4__soft_delete_columns.sql` | `moment / moment_like / moment_comment / diary` 加 `deleted_at` |
| V5 | `V5__chat_message_rebuild.sql` | `chat_message` 重建为 `sender_id/receiver_id`，并迁移老聊天到 `chat_message_v1` |
| V6 | `V6__chat_message_media_list.sql` | `chat_message.media_list` |

## 不同场景下怎么部署

### A. 云端数据库——已经手工跑过 migrate-2026-06-14.sql（schema 已经是最新）

数据库已经有 V2..V6 的所有列，只是 Flyway 不知道。这种情况要**让 Flyway 把当前数据库标记为「已经到 V6」，下次新加 V7 才会跑**。

服务器上 `.env` 加上：

```env
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=true
FLYWAY_BASELINE_VERSION=6
```

然后 `docker compose up -d --build app`。

第一次启动时 Flyway 会：
1. 在数据库里建 `flyway_schema_history` 表
2. 因为表里没记录、库不空，触发 `baseline-on-migrate`：插入一条 baseline 记录 `version=6`
3. 后续 V7、V8 才会被执行

### B. 云端数据库——还没跑 migrate-2026-06-14.sql（schema 仍是最初版）

数据库现在还是 V1 的样子。让 Flyway 帮你跑 V2..V6。

`.env`：

```env
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=true
FLYWAY_BASELINE_VERSION=1
```

启动时 Flyway 会：
1. baseline 标记 V1
2. 顺序执行 V2 → V3 → V4 → V5 → V6
3. **包含 V5 的 chat_message 重建 + 旧数据迁移**

### C. 全新空库（新服务器 / 新环境）

`.env`：

```env
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=true
FLYWAY_BASELINE_VERSION=0
```

启动时 Flyway 从 V1 一路跑到 V6，建好整套表 + 种子用户。

> docker-compose 里挂载 `init.sql` 到 `docker-entrypoint-initdb.d` 的那一行已经去掉，schema 由 Flyway 全权管理。

## 上线步骤（最常用 = 场景 A）

```bash
# 1. 服务器项目目录
cd /your/path/to-my-l

# 2. 备份数据库
docker exec to-my-l-mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" to_my_l' \
  > backup-$(date +%Y%m%d-%H%M).sql

# 3. 拉代码
git pull

# 4. 编辑 .env，把这三行设上
nano .env
#    FLYWAY_ENABLED=true
#    FLYWAY_BASELINE_ON_MIGRATE=true
#    FLYWAY_BASELINE_VERSION=6    # 看你属于哪种场景

# 5. 重建并重启 app
docker compose build app
docker compose up -d app

# 6. 看日志
docker compose logs -f app
#    成功标志:
#      "Successfully validated N migrations"
#      "Schema baseline created" 或 "Successfully applied N migrations"

# 7. 验证
curl -i http://localhost:8081/diary/days
#    期望: {"code":401,"msg":"Please login first"}

# 8. 看 flyway_schema_history
docker exec -it to-my-l-mysql sh -c \
  'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" to_my_l \
   -e "SELECT version, description, type, success FROM flyway_schema_history;"'
```

## 以后再加迁移

直接在 [src/main/resources/db/migration/](src/main/resources/db/migration/) 下新建 `V7__xxx.sql`，commit、push、`git pull` + 重启 app，Flyway 自动跑 V7。

不要回头改已发布的 V*——会触发 Flyway 的 checksum 校验失败。

## 出错了怎么办

### 启动报 "Validate failed"

某个已应用 V* 的内容被改过。修法：
- 要么把那个 V* 还原到部署时的内容
- 要么 `DELETE FROM flyway_schema_history WHERE version='X';` 让它重跑（**仅在能确认重跑安全时**）

### V5 重跑失败

V5 里的 `RENAME TABLE chat_message TO chat_message_v1` 如果 `chat_message_v1` 已经存在会失败：

```sql
SHOW TABLES LIKE 'chat_message%';
DROP TABLE IF EXISTS chat_message_v1;  -- 确认里面没要保留的数据再删
DELETE FROM flyway_schema_history WHERE version='5';
```

然后重启 app 容器。

### 想暂时不让 Flyway 起作用

`.env` 里 `FLYWAY_ENABLED=false`，重启 app。schema 不会被动到。