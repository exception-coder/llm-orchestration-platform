# LLM Orchestration Platform

企业级 LLM 能力编排和管理平台

## 技术栈

- **Spring Boot 3.4.5** + **Spring AI 1.0.1** + **Spring AI Alibaba 1.0.0.4**
- **Java 17** / **Maven**
- **Qdrant** 向量数据库
- **Vue 3** + Element Plus + Pinia + Vite 前端

## 架构设计

DDD-lite 多模块分层架构，依赖方向严格单向：

```
llm-api → llm-application → llm-domain ← llm-infrastructure
                                          ↑
                                    llm-starter（Spring Boot 入口）
```

| 模块 | 职责 |
|---|---|
| `llm-domain` | 核心业务模型、仓储接口、领域服务接口 |
| `llm-application` | UseCase + Service，编排领域和仓储 |
| `llm-api` | REST Controller（按业务域分包）、DTO |
| `llm-infrastructure` | LLM Provider、Agent 框架、向量存储、数据库仓储 |
| `llm-starter` | Spring Boot 启动类、配置、配置验证 |
| `llm-frontend` | Vue 3 前端 |

## 核心功能

### 多 Provider 智能路由

| Provider | 协议 | 用途 |
|---|---|---|
| 智谱 AI（GLM） | OpenAI 兼容 | Chat + Embedding，默认主 Provider |
| 阿里百炼（Qwen/DeepSeek） | DashScope | Chat，降级备选 |
| Ollama | Ollama 原生 | 本地模型 |

- Guava RateLimiter 令牌桶限速（按平台配置 RPM）
- 主 Provider 429 时自动按 `fallback-order` 降级

### 智能体编排

基于自研 Agent 框架，支持 Tool 自动扫描注册、Graph 多节点编排、SSE 流式执行追踪。

**已注册智能体：**

| Graph | 名称 | 说明 |
|---|---|---|
| `devplan` | 开发计划智能体 | 四阶段流程：代码感知 → 需求分析 → 方案设计 → 质量评审 |
| `secretary` | 个人秘书智能体 | 日程管理、待办管理、笔记检索 |

### 其他功能

- **对话管理** — 普通 + SSE 流式对话，上下文记忆
- **Prompt 模板** — 数据库管理、分类、测试对比
- **笔记系统** — AI 自动分类、加密存储、语义搜索
- **内容优化** — 多平台（小红书/抖音/微博等）、多风格适配
- **职位搜索** — 基于 Qdrant 的向量相似度检索
- **文档浏览** — 文档目录解析、AI 语义检索
- **模型配置** — 运行时 LLM 模型 CRUD 管理

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- (可选) Qdrant — `docker/qdrant/docker-compose.yml`
- (可选) Ollama

### 配置

复制并编辑环境配置：

```bash
# 分环境配置位于
llm-starter/src/main/resources/config/dev/
├── datasource.yml    # 数据源
├── spring-ai.yml     # Spring AI、Qdrant、限速降级
└── logging.yml       # 日志
```

关键环境变量：

```bash
ZHIPU_API_KEY=xxx          # 智谱 AI
DASHSCOPE_API_KEY=xxx      # 阿里百炼
OLLAMA_BASE_URL=http://localhost:11434
```

### 构建与运行

```bash
# 后端
mvn clean install
cd llm-starter && mvn spring-boot:run
# 服务启动在 http://localhost:8080

# 前端
cd llm-frontend
npm install && npm run dev
```

## API 示例

### 对话

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "conv-001",
    "message": "你好",
    "provider": "zhipu",
    "model": "glm-4-flash"
  }'
```

### 内容优化

```bash
curl -X POST http://localhost:8080/api/v1/content/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "originalContent": "今天试用了一款新的护肤品，效果还不错。",
    "platform": "XIAOHONGSHU",
    "style": "CASUAL",
    "contentType": "PRODUCT_REVIEW"
  }'
```

### 主要 API 路径

| 路径前缀 | 功能 |
|---|---|
| `/api/v1/chat` | 对话（含流式） |
| `/api/v1/agents` | Agent 管理与执行 |
| `/api/v1/graphs` | Graph 编排 |
| `/api/v1/tools` | Tool 管理 |
| `/api/v1/prompt-templates` | Prompt 模板 |
| `/api/v1/model-config` | 模型配置 |
| `/api/v1/notes` | 笔记 |
| `/api/v1/secretary` | 个人秘书 |
| `/api/v1/content` | 内容优化 |
| `/api/v1/dev-plan` | 开发计划生成 |
| `/api/job-search` | 职位搜索 |

## 扩展指南

### 添加新的 LLM Provider

1. 在 `llm-infrastructure` 实现 `LLMProvider` 接口
2. 实现 `chat()`、`stream()`、`getChatModel()`、`getProviderName()`、`supports()` 方法
3. 添加 `@Component`，自动注册
4. 在 `application-llm.yml` 中添加对应配置和 `rate-limit.rpm`

### 添加新的 Tool

1. 创建类并在方法上标注 `@Tool(name, description, tags)`
2. 参数用 `@ToolParam` 注解
3. 启动时 `ToolScanner` 自动扫描注册

### 添加新的智能体

1. 创建 Initializer 类，标注 `@AgentGroup(id, name, description)`
2. 实现 `AgentGroupProvider` 接口
3. 在 `onApplicationEvent` 中注册 Agent 定义
4. `AgentGroupScanner` 自动创建 Graph 并绑定节点

## License

MIT License
