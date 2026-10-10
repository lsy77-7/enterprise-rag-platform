# enterprise-rag-platform

基于 **Spring Boot + RAG** 的企业智能知识库问答系统。

这是一个可实际运行的 Java 后端项目，完整实现了「登录 → 创建知识库 → 上传 PDF → 解析分块 → Embedding → 向量检索 → 大模型生成回答 → 引用来源」的闭环，并在此基础上补充了多用户、知识库权限、多轮对话和 Vue 前端。

## 解决的问题

- 企业内部文档（PDF）分散、难以快速检索与提问。
- 传统关键词搜索无法理解语义，检索结果不精准。
- 大模型问答容易“编造”，需要基于企业文档的检索增强（RAG）来提供可溯源的回答。
- 需要按用户/知识库做数据隔离，普通用户不能越权访问他人知识库。

## 技术栈

**后端**

- Java 17
- Spring Boot 3.5.16
- Maven（单模块）
- Spring Web（含 `RestClient`，用于调用外部 LLM / Embedding 的 OpenAI 兼容 API）
- Spring Data JPA + MySQL
- Flyway 数据库迁移
- Spring Security（Session 会话认证 + 方法级权限）
- Spring Validation、Lombok、Spring Boot Actuator
- Springdoc OpenAPI / Swagger UI
- Apache PDFBox 3.0.4（PDF 文本解析）

**前端**

- Vue 3 + Vite + Axios

**外部 AI 服务**

- LLM：DeepSeek（OpenAI 兼容协议）
- Embedding：SiliconFlow `BAAI/bge-m3`（OpenAI 兼容协议）

**向量存储**

- 本地 JSON 文件向量存储（每个知识库一个 `data/vector-store/{kbId}.json`，余弦相似度检索），不依赖 pgvector / Milvus / Elasticsearch 等外部组件。

## 已实现功能

- 用户登录（Session 会话）、当前用户信息、退出登录
- 用户管理：分页列表、新增、批量新增、修改角色/状态、删除（ADMIN）
- 知识库管理：创建、查询、删除，公有/私有区分，列表可折叠、搜索与筛选
- 知识库授权：单用户授权/取消授权、批量授权/批量取消授权（ADMIN）
- 文档管理：PDF 上传（支持批量）、文档列表、删除、重新索引
- 文档解析链路：PDFBox 按页解析 → 文本分块 → `document_chunk` 持久化 → 异步 Embedding → 写入向量索引
- 文档生命周期状态：`UPLOADED / PARSING / INDEXING / COMPLETED / FAILED`
- RAG 检索：用户问题向量化 → Top-K 余弦相似度检索（按知识库隔离）
- RAG 问答：检索结果拼接 Prompt → DeepSeek 生成答案 → 返回引用来源（页码 + Chunk）
- 多轮对话：创建会话、保存历史、最近 6 条上下文、会话自动命名、手动重命名、删除会话
- 权限隔离：普通用户只能访问被授权的知识库，后端接口校验（不只靠前端隐藏）

## 系统架构

前后端分离，后端为模块化单体，按 Controller / Service / Repository / Infrastructure 分层；外部 AI 能力通过 `EmbeddingService`、`LlmService`、`VectorStoreService` 等接口抽象，便于替换。

完整架构图见 [docs/architecture.md](docs/architecture.md)。

## 核心 RAG 流程

```text
PDF 上传
→ PDFBox 解析（按页提取文本）
→ 文本分块（chunkSize=1000, overlap=200）
→ SiliconFlow BGE-M3 Embedding
→ 写入本地 JSON 向量存储

用户提问
→ 问题 Embedding
→ Top-K 余弦相似度检索（按知识库隔离）
→ 拼接上下文 + 历史对话
→ DeepSeek 生成回答
→ 返回答案 + 引用来源
```

详细流程图见 [docs/rag-flow.md](docs/rag-flow.md)。

## 目录结构

```text
enterprise-rag-platform/
├── pom.xml
├── README.md
├── .env.example
├── docs/
│   ├── architecture.md
│   ├── rag-flow.md
│   ├── api-guide.md
│   ├── setup.md
│   └── images/
├── data/
│   ├── documents/        # 上传的 PDF（运行时生成，不入库）
│   └── vector-store/     # 本地 JSON 向量索引（运行时生成，不入库）
├── frontend/             # Vue 3 + Vite
│   └── src/
│       ├── App.vue
│       ├── AdminPanel.vue
│       ├── api.js
│       └── main.js
└── src/
    ├── main/java/com/example/rag/
    │   ├── controller/       # REST API
    │   ├── service/          # 业务逻辑
    │   ├── repository/       # Spring Data JPA
    │   ├── entity/           # JPA 实体
    │   ├── dto/              # 请求/响应 DTO
    │   ├── infrastructure/   # embedding/llm/parser/splitter/storage/vectorstore
    │   ├── common/           # 统一响应、全局异常
    │   ├── security/         # Spring Security 配置
    │   └── config/           # 异步执行器
    └── resources/
        ├── application.yml
        └── db/migration/     # Flyway V1~V8
```

## 环境要求

- JDK 17
- Maven 3.9+
- MySQL 8.x
- Node.js 18+（运行前端时）

详细安装与启动见 [docs/setup.md](docs/setup.md)。

## 环境变量

安全起见，敏感配置通过环境变量注入，参考 [.env.example](.env.example)。

| 变量                                                                                                         | 说明                                             |
| ------------------------------------------------------------------------------------------------------------ | ------------------------------------------------ |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD`                                            | MySQL 连接信息                                   |
| `EMBEDDING_ENABLED` / `EMBEDDING_BASE_URL` / `EMBEDDING_API_KEY` / `EMBEDDING_MODEL` / `EMBEDDING_DIMENSION` | Embedding 服务                                   |
| `LLM_ENABLED` / `LLM_BASE_URL` / `LLM_API_KEY` / `LLM_MODEL`                                                 | LLM 服务                                         |
| `VITE_API_TARGET`                                                                                            | 前端开发代理目标（默认 `http://localhost:8081`） |

## 启动步骤

后端：

```powershell
# 1. 创建数据库
mysql -uroot -p -e "CREATE DATABASE enterprise_rag CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 编译测试
mvn clean test

# 3. 启动（默认 8081）
mvn spring-boot:run
```

前端：

```powershell
cd frontend
npm install
npm run dev
```

## 访问地址

- 前端：<http://localhost:5173>
- 后端健康检查：<http://localhost:8081/api/health>
- Swagger UI：<http://localhost:8081/swagger-ui/index.html>
- OpenAPI JSON：<http://localhost:8081/v3/api-docs>

## 主要 API

按模块整理的完整接口文档见 [docs/api-guide.md](docs/api-guide.md)。

## 演示截图

| 截图           | 路径                                   |
| -------------- | -------------------------------------- |
| 登录页面       | `docs/images/login.png`                |
| 知识库列表     | `docs/images/knowledge-base-list.png`  |
| PDF 上传与索引 | `docs/images/upload-index.png`         |
| 问答结果与来源 | `docs/images/qa-sources.png`           |
| 对话历史       | `docs/images/conversation-history.png` |
| 权限管理       | `docs/images/permission.png`           |

## 当前已知限制

- 认证为 **Session 会话**（Spring Security + HttpSession），**尚未实现 JWT**。
- 向量存储为**本地 JSON 文件**，单机可用，不适合大规模生产环境；替换为真实向量数据库时只需实现 `VectorStoreService`。
- 仅支持 **PDF** 上传，暂不支持 Word / Markdown。
- 检索为纯向量余弦相似度，**未实现 Hybrid Search、Rerank、Query Rewrite**。
- 回答为**非流式**，未实现 SSE 流式输出。
- 未引入 Redis、消息队列、Elasticsearch、MinIO、Docker 部署等。
- 未实现 RAG 可观测性 Trace 与自动评测（Evaluation）。
- `application.yml` 中的数据库用户名/密码当前为硬编码默认值，提交前请改用环境变量。

## 文档索引

- [系统架构](docs/architecture.md)
- [RAG 流程](docs/rag-flow.md)
- [接口文档](docs/api-guide.md)
- [环境配置与启动](docs/setup.md)
