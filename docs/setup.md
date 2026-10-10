# 环境配置与启动

## 1. 环境要求

- JDK 17
- Maven 3.9+
- MySQL 8.x
- Node.js 18+（运行前端时）

## 2. 数据库初始化

```sql
CREATE DATABASE enterprise_rag
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

表结构由 Flyway 自动迁移（`src/main/resources/db/migration/V1~V8`），首次启动会自动建表：

- `sys_user`
- `knowledge_base`
- `document`
- `document_chunk`
- `conversation`
- `chat_message`
- `user_knowledge_base`

## 3. 环境变量

项目使用环境变量注入敏感配置（参考根目录 [.env.example](../.env.example)）。

> 注意：本项目**不会自动读取 `.env` 文件**，需要在启动前手动 `export`（Linux/macOS）或 `set`（Windows PowerShell）这些变量。

### 数据库

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=enterprise_rag
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
```

### Embedding（SiliconFlow）

```text
EMBEDDING_ENABLED=true
EMBEDDING_BASE_URL=https://api.siliconflow.cn/v1
EMBEDDING_API_KEY=your_siliconflow_api_key
EMBEDDING_MODEL=BAAI/bge-m3
EMBEDDING_DIMENSION=1024
```

### LLM（DeepSeek）

```text
LLM_ENABLED=true
LLM_BASE_URL=https://api.deepseek.com
LLM_API_KEY=your_deepseek_api_key
LLM_MODEL=deepseek-chat
```

## 4. 后端启动

```powershell
# 编译并运行测试
mvn clean test

# 启动（默认端口 8081）
mvn spring-boot:run
```

如果 8081 被占用：

```powershell
mvn "-Dspring-boot.run.arguments=--server.port=8082" spring-boot:run
```

健康检查：<http://localhost:8081/api/health>

## 5. 前端启动

```powershell
cd frontend
npm install
npm run dev
```

- 前端开发服务器默认端口 5173：<http://localhost:5173>
- 开发环境通过 Vite 代理把 `/api` 转发到 `http://localhost:8081`（可用 `VITE_API_TARGET` 覆盖）。

生产构建：

```powershell
npm run build
```

## 6. 首次登录 / 默认管理员

- 认证为 Session 会话，密码使用 BCrypt（委托编码器）存储于 `sys_user.password_hash`。
- 代码中存在 `DefaultUserService`，会在**文档上传**时懒创建默认 `admin` 账号（默认密码 `admin123`）；**Flyway 迁移中没有种子管理员数据**。
- 因此全新数据库首次登录前，建议先手动插入管理员，或先触发一次文档上传创建 `admin`。具体以实际环境为准（待验证）。

手动创建管理员示例（仅供参考）：

```sql
INSERT INTO sys_user (username, password_hash, display_name, role, status)
VALUES ('admin', '{noop}admin123', '系统管理员', 'ADMIN', 'ACTIVE');
```

> 示例仅用于本地开发；生产环境请使用 BCrypt 哈希，不要使用 `{noop}` 明文。

## 7. 常见问题

### 7.1 端口被占用

`netstat -ano | findstr :8081` 找到占用进程后处理，或用 `--server.port` 换端口。

### 7.2 调用 SiliconFlow / DeepSeek 报「Permission denied: no further information」

这是 Windows 防火墙/安全软件拦截了 `java.exe` 的出站连接（`WSAEACCES`），不是网络或代码问题。请为 JDK 的 `java.exe` 添加出站放行规则。

### 7.3 Embedding / LLM 未配置

- 未开启 `EMBEDDING_ENABLED` / `LLM_ENABLED` 时，应用仍可启动，但对应能力会被禁用。
- 上传后若 Embedding 未配置，文档会进入 `FAILED` 状态并记录 `error_message`。
- 调用 RAG 时若 LLM 未配置，接口返回 503。

### 7.4 大文档上传超时

Embedding 与向量写入已改为事务提交后异步执行，HTTP 请求只同步完成「保存 → 解析 → 分块 → Chunk 入库」；如仍超时，可提高前端 `api.js` 中的 `timeout`。

### 7.5 上传后文档一直显示「索引中」

确认 `EMBEDDING_ENABLED=true` 且 `EMBEDDING_API_KEY` 有效；索引任务在后台线程池 `vector-index-*` 中执行，异常时会更新为 `FAILED`。

## 8. 安全提醒

- 根目录 `.env.example` 只包含变量名与占位值。
- 当前 `application.yml` 中数据库用户名/密码为硬编码默认值，提交前建议改为 `${DB_USERNAME:root}` / `${DB_PASSWORD:}` 形式。
