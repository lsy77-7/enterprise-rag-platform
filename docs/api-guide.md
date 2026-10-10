# 接口文档

> 本文档根据当前代码中的 Controller / DTO / 请求映射整理，接口以实际运行为准；未完全验证的内容已标注「待验证」。

## 约定

- 基础路径：`http://localhost:8081`
- 统一响应体：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- 认证方式：**Session 会话**（登录后通过 Cookie 维持，非 JWT）。
- 权限说明：
  - 「登录」= 已认证用户即可访问（`/api/**` 默认要求登录）。
  - 「访问权限」= 知识库创建者 / 管理员 / 被授权（READ 或 EDIT）用户。
  - 「编辑权限」= 知识库创建者 / 管理员 / 被授权为 EDIT 的用户。
  - 「ADMIN」= `@PreAuthorize("hasRole('ADMIN')")`。

## 1. 登录认证

### POST /api/v1/auth/login

公开接口。

请求：

```json
{
  "username": "admin",
  "password": "your_password"
}
```

响应 `data`：

```json
{
  "id": 1,
  "username": "admin",
  "nickname": "系统管理员",
  "role": "ADMIN",
  "status": "ACTIVE"
}
```

### GET /api/v1/auth/me

公开接口。未登录时返回 `code=401`。

响应 `data` 同登录接口的用户对象。

### POST /api/v1/auth/logout

公开接口。使当前 Session 失效。

## 2. 健康检查

### GET /api/health

公开接口。

```json
{
  "code": 200,
  "message": "success",
  "data": "enterprise-rag is running"
}
```

## 3. 知识库

基础路径：`/api/v1/knowledge-bases`

### POST /api/v1/knowledge-bases

登录。创建知识库。公有/私有由创建者角色决定（ADMIN 创建为 PUBLIC，普通用户创建为 PRIVATE）。

请求：

```json
{
  "name": "企业制度知识库",
  "description": "公司内部制度文档"
}
```

响应 `data`：

```json
{
  "id": 1,
  "name": "企业制度知识库",
  "description": "公司内部制度文档",
  "visibility": "PUBLIC",
  "ownerId": 1,
  "createdAt": "2026-01-01T00:00:00"
}
```

### GET /api/v1/knowledge-bases

登录。普通用户返回「自己创建的 + 被授权的」知识库；ADMIN 返回全部。

响应 `data`：`KnowledgeBaseResponse` 数组。

### GET /api/v1/knowledge-bases/{id}

登录 + 访问权限。

### DELETE /api/v1/knowledge-bases/{id}

登录 + 知识库创建者或 ADMIN。删除知识库、文档、Chunk、向量索引及本地 PDF。

### POST /api/v1/knowledge-bases/{kbId}/reindex

编辑权限。重建整个知识库向量索引（异步）。

响应 `data`：

```json
{
  "knowledgeBaseId": 1,
  "message": "向量索引任务已提交，正在后台处理"
}
```

### POST /api/v1/knowledge-bases/{kbId}/grants/{userId}

ADMIN。单用户授权。查询参数 `permission=READ|EDIT`（默认 READ）。

### DELETE /api/v1/knowledge-bases/{kbId}/grants/{userId}

ADMIN。单用户取消授权。

### GET /api/v1/knowledge-bases/{kbId}/grants

ADMIN。查看当前已授权用户。

响应 `data`：`KnowledgeBaseGrantResponse` 数组，字段 `userId / username / nickname / permission`。

### POST /api/v1/knowledge-bases/{kbId}/permissions/batch

ADMIN。批量授权。

请求：

```json
{
  "userIds": [2, 3],
  "permission": "READ"
}
```

响应 `data`：

```json
{
  "grantedCount": 2,
  "invalidCount": 0,
  "invalidUserIds": []
}
```

### DELETE /api/v1/knowledge-bases/{kbId}/permissions/batch

ADMIN。批量取消授权。

请求：

```json
{
  "userIds": [2, 3]
}
```

响应 `data`：

```json
{
  "revokedCount": 2
}
```

## 4. 文档

基础路径：`/api/v1/knowledge-bases/{kbId}/documents`

### POST /api/v1/knowledge-bases/{kbId}/documents

编辑权限。上传 PDF，`multipart/form-data`，字段名 `file`。接口内同步完成保存、解析、分块、Chunk 入库，Embedding/索引异步执行。

响应 `data`：

```json
{
  "id": 10,
  "fileName": "员工手册.pdf",
  "status": "INDEXING",
  "errorMessage": null
}
```

### GET /api/v1/knowledge-bases/{kbId}/documents

访问权限。查询该知识库文档列表。

响应 `data`：`DocumentResponse` 数组，字段 `id / fileName / status / pageCount / errorMessage / createdAt`。

### DELETE /api/v1/knowledge-bases/{kbId}/documents/{documentId}

编辑权限。删除文档及其 Chunk、向量、本地文件。

### POST /api/v1/knowledge-bases/{kbId}/documents/{documentId}/reindex

访问权限（注意：当前实现为访问权限而非编辑权限）。重新索引单个文档（异步）。

响应 `data`：

```json
{
  "documentId": 10,
  "message": "文档重新索引任务已提交，正在后台处理"
}
```

## 5. 检索与问答

### POST /api/v1/retrieval/search

登录 + 访问权限。向量检索 Top-K。

请求：

```json
{
  "knowledgeBaseId": 1,
  "query": "员工年假怎么休？",
  "topK": 5
}
```

响应 `data`：`RetrievalSearchResponse` 数组，字段 `chunkId / documentId / knowledgeBaseId / chunkIndex / pageNumber / score / content`。

### POST /api/v1/rag/chat

登录 + 访问权限。单次 RAG 问答。

请求：

```json
{
  "knowledgeBaseId": 1,
  "question": "员工年假怎么休？",
  "topK": 3
}
```

响应 `data`：

```json
{
  "answer": "根据公司制度，员工年假……",
  "sources": [
    {
      "chunkId": 100,
      "documentId": 10,
      "knowledgeBaseId": 1,
      "chunkIndex": 0,
      "pageNumber": 3,
      "score": 0.82,
      "content": "……"
    }
  ]
}
```

## 6. 多轮对话

基础路径：`/api/v1/conversations`

### POST /api/v1/conversations

登录 + 访问权限。创建会话。

请求：

```json
{
  "knowledgeBaseId": 1,
  "title": "新对话"
}
```

响应 `data`：`ConversationResponse`，字段 `id / knowledgeBaseId / title / manualRenamed / createdAt / updatedAt`。

### GET /api/v1/conversations

登录。查询当前用户自己的会话列表。

### GET /api/v1/conversations/{id}

登录 + 会话所有者。查询会话详情及历史消息。

响应 `data`：`ConversationDetailResponse`（含 `messages`，每条 `ChatMessageResponse` 字段 `id / role / content / createdAt`）。

### PATCH /api/v1/conversations/{id}

登录 + 会话所有者。重命名会话。

请求：

```json
{
  "title": "员工年假咨询"
}
```

### DELETE /api/v1/conversations/{id}

登录 + 会话所有者。删除会话及其聊天记录。

### POST /api/v1/conversations/{id}/chat

登录 + 会话所有者 + 访问权限。多轮 RAG 问答（自动携带最近 6 条历史）。

请求：

```json
{
  "question": "那病假呢？",
  "topK": 3
}
```

响应 `data`：`RagChatResponse`（`answer` + `sources`），同单次问答。

## 7. 用户管理（ADMIN）

基础路径：`/api/v1/users`，全部接口要求 ADMIN。

### GET /api/v1/users

分页查询。查询参数：`page`（默认 0）、`size`（默认 20）、`username`、`role`、`status`。

响应 `data`：`UserPageResponse`，字段 `content / totalElements / totalPages / page / size`。

### POST /api/v1/users

新增用户。

请求：

```json
{
  "username": "zhangsan",
  "password": "123456",
  "nickname": "张三",
  "role": "USER",
  "status": "ACTIVE"
}
```

响应 `data`：`UserResponse`，字段 `id / username / nickname / role / status / createdAt / updatedAt`。

### POST /api/v1/users/batch

批量新增。请求 `{ "users": [ { ...UserCreateRequest } ] }`，响应 `BatchUserCreateResult`，字段 `total / successCount / failureCount / failures`。

### PUT /api/v1/users/{id}

修改用户。请求字段 `nickname / role / status`。

### DELETE /api/v1/users/{id}

删除用户及其会话数据。后端会阻止删除「当前登录管理员」与「系统最后一个有效管理员（ADMIN + ACTIVE）」。

## 待验证事项

- 全新数据库首次登录前，默认管理员账号的初始化方式（当前通过 `DefaultUserService` 在文档上传时懒创建，非 Flyway 种子，详见 [setup.md](setup.md)）。
