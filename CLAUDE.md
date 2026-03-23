# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend

```bash
# 构建全量项目
mvn clean install

# 启动服务
cd llm-starter && mvn spring-boot:run

# 运行测试
mvn test

# 运行单个测试类
mvn test -pl <module> -Dtest=XxxTest
```

服务默认运行在 `http://localhost:8080`

### Frontend

```bash
cd llm-frontend
npm install
npm run dev      # 开发模式
npm run build    # 生产构建
```

## 架构

DDD-lite 多模块 Maven 项目，依赖方向严格单向：

```
llm-api → llm-application → llm-domain ← llm-infrastructure
                                          ↑
                                    llm-starter（Spring Boot 入口）
```

| 模块 | 职责 |
|---|---|
| `llm-domain` | 核心业务模型（`model/`）、仓储接口（`repository/`）、领域服务接口（`service/`） |
| `llm-application` | UseCase 接口 + Service 实现，编排领域和仓储 |
| `llm-api` | REST Controller、DTO、`GlobalExceptionHandler` |
| `llm-infrastructure` | LLM Provider 实现、Qdrant 向量存储、数据库仓储实现 |
| `llm-starter` | Spring Boot 启动类、`application.yml` 配置 |
| `llm-frontend` | Vue 3 + Element Plus + Pinia 前端 |

### 关键接口

- `LLMProvider`（domain）：所有 LLM 提供商实现此接口（OpenAI via Spring AI、Ollama via LangChain4j）
- `LLMOrchestrationService`：根据请求中的 `provider` 字段路由到对应 `LLMProvider`
- `VectorStoreRepository`（domain）：Qdrant 向量操作抽象
- `PromptTemplate`（domain）：Prompt 模板领域接口

## 编码规范（关键约束）

### 技术选型优先级

1. Spring 官方组件（`spring-ai-qdrant-store`、`spring-ai-openai`）
2. 提供 Spring Boot Starter 的第三方库
3. 原生 SDK（仅在 Spring 无支持时）

### 强制规则

- **禁止** 创建 `XxxExample.java`、`XxxDemo.java`、`XxxSample.java` 等示例类；使用示例写在 JavaDoc 或 `/docs/` Markdown 文档中
- **必须** 使用 `jakarta.*` 命名空间，不用 `javax.*`
- **必须** 构造函数注入，禁止 `@Autowired` 字段注入
- `@RefreshScope` 的 Bean 之间不允许相互注入；每个 Bean 从配置直接创建依赖
- 动态刷新配置调用 `refreshScope.refreshAll()` + `publishEvent(new EnvironmentChangeEvent(keys))`
- Qdrant 批量写入用 `Arrays.asList()` 避免与 `io.qdrant.client.grpc.Collections` 冲突

### 日志

使用 SLF4J `LoggerFactory.getLogger()`，禁止 `System.out.println`。

## 开发日志

**每次代码变更必须追加** `docs/dev-log.md`，包含：日期、任务描述、变更文件、关键设计决策。

## 配置

主配置文件：`llm-starter/src/main/resources/application.yml`

关键配置项：
- `llm.openai.api-key`
- `llm.ollama.base-url`（默认 `http://localhost:11434`）
- Qdrant：`docker/qdrant/docker-compose.yml` 启动本地实例

## Git 提交规范

```
feat / fix / docs / refactor / test / chore
```
