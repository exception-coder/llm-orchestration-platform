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

### 分层依赖强制规则

- **禁止** `llm-application` 或 `llm-domain` 中出现 `import com.exceptioncoder.llm.infrastructure.*`
- **禁止** `llm-domain` 中出现 `import com.exceptioncoder.llm.application.*`
- application 层需要调用 infrastructure 层的能力时，**必须**在 domain 层定义接口（`service/` 或 `repository/`），由 infrastructure 层实现，application 层依赖接口
- 发现已有违规代码时，修复方式：domain 层抽接口 → infrastructure 层 `implements` → application 层改为依赖接口

### 强制规则

- **禁止** 创建 `XxxExample.java`、`XxxDemo.java`、`XxxSample.java` 等示例类；使用示例写在 JavaDoc 或 `/docs/` Markdown 文档中
- **必须** 使用 `jakarta.*` 命名空间，不用 `javax.*`
- **必须** 构造函数注入，禁止 `@Autowired` 字段注入
- `@RefreshScope` 的 Bean 之间不允许相互注入；每个 Bean 从配置直接创建依赖
- 动态刷新配置调用 `refreshScope.refreshAll()` + `publishEvent(new EnvironmentChangeEvent(keys))`
- Qdrant 批量写入用 `Arrays.asList()` 避免与 `io.qdrant.client.grpc.Collections` 冲突

### 日志

使用 SLF4J `LoggerFactory.getLogger()`，禁止 `System.out.println`。

## 开发流程强制规定

每次功能讨论结束、进入实现前，必须先调用 `team-standards:design-doc-required` skill，检查或创建设计文档，然后再进入实现计划。

## 开发日志

**每次代码变更必须追加**到对应周的 `docs/dev/dev-log-YYYY-WNN.md`（ISO 8601 周次），包含：日期、任务描述、变更文件、关键设计决策。

- 索引文件：`docs/dev/dev-log.md`（列出各周文件链接，勿直接写内容）
- 当周文件不存在时新建，例如 `docs/dev/dev-log-2026-W15.md`，并在索引中补一行
- 周次计算：Python `date.isocalendar()` 或 `date +%G-W%V`

## 踩坑日志

遇到依赖冲突、版本不兼容、隐蔽 bug 等问题时，**必须**追加记录到 `docs/dev/pitfall-spring-ai-version.md`，格式为 `## 坑 N：标题` → 现象 → 原因 → 解决。

## 配置

主配置文件：`llm-starter/src/main/resources/application.yml`

关键配置项：
- `llm.openai.api-key`
- `llm.ollama.base-url`（默认 `http://localhost:11434`）
- Qdrant：`docker/qdrant/docker-compose.yml` 启动本地实例

## 智能体 / Agent / Tool 注册规范

新增或修改智能体、Agent、Tool 时，**必须**同步更新本节索引，并在对应类上补全 JavaDoc 场景注解。

### JavaDoc 场景注解格式（Tool 类必须包含）

```java
/**
 * 一句话功能描述。
 *
 * <p><b>归属智能体：</b>xxx智能体（graph-id）
 * <br><b>归属 Agent：</b>xxx（agent-id）
 * <br><b>调用阶段：</b>第N阶段 — 阶段名
 * <br><b>业务场景：</b>什么角色在什么情况下调用，解决什么问题。
 */
```

### 智能体（Graph）索引

| Graph ID | 名称 | Initializer 类 | 模块 |
|---|---|---|---|
| `devplan` | 开发计划智能体 | `infrastructure/devplan/config/DevPlanAgentInitializer` | llm-infrastructure |
| `secretary` | 个人秘书智能体 | `infrastructure/config/SecretaryAgentInitializer` | llm-infrastructure |

### Agent 索引

| Agent ID | 名称 | 归属智能体 | 角色/职责 |
|---|---|---|---|
| `devplan-code-awareness` | 代码感知分析专家 | devplan | 扫描项目结构、解析依赖、分析代码、建立索引 |
| `devplan-requirement-analyzer` | 需求分析专家 | devplan | 分析需求影响范围、定位涉及的现有类 |
| `devplan-solution-architect` | 方案架构师 | devplan | 基于画像和影响分析生成设计文档 |
| `devplan-plan-reviewer` | 方案审查专家 | devplan | 四维度质量评审（完整性/一致性/可行性/规范性） |
| `secretary-default` | 个人秘书 | secretary | 日程管理、待办管理、笔记检索 |

### Tool 索引

| Tool ID | 名称 | 归属 Agent | 实现类 |
|---|---|---|---|
| `devplan_project_scan` | 项目结构扫描 | devplan-code-awareness | `infrastructure/devplan/tool/ProjectScanTool` |
| `devplan_dependency_analysis` | 依赖分析 | devplan-code-awareness | `infrastructure/devplan/tool/DependencyAnalysisTool` |
| `devplan_code_structure` | 代码结构分析 | devplan-code-awareness | `infrastructure/devplan/tool/CodeStructureAnalysisTool` |
| `devplan_config_scan` | 配置扫描 | devplan-code-awareness | `infrastructure/devplan/tool/ConfigScanTool` |
| `devplan_code_index` | 代码向量索引 | devplan-code-awareness | `infrastructure/devplan/tool/CodeIndexTool` |
| `devplan_profile_index` | 画像向量索引 | devplan-code-awareness | `infrastructure/devplan/tool/ProfileIndexTool` |
| `devplan_code_search` | 代码语义搜索 | devplan-requirement-analyzer, devplan-solution-architect | `infrastructure/devplan/tool/CodeSearchTool` |
| `devplan_profile_search` | 画像语义检索 | devplan-requirement-analyzer, devplan-solution-architect | `infrastructure/devplan/tool/ProfileSearchTool` |
| `devplan_file_read` | 文件读取 | devplan-requirement-analyzer | `infrastructure/devplan/tool/FileReadTool` |
| `devplan_template_render` | 模板渲染 | devplan-solution-architect | `infrastructure/devplan/tool/TemplateRenderTool` |
| `schedule_add` | 添加日程 | secretary-default | `infrastructure/agent/tool/builtin/ScheduleTool` |
| `schedule_list` | 查看日程 | secretary-default | `infrastructure/agent/tool/builtin/ScheduleTool` |
| `schedule_done` | 完成日程 | secretary-default | `infrastructure/agent/tool/builtin/ScheduleTool` |
| `todo_add` | 添加待办 | secretary-default | `infrastructure/agent/tool/builtin/TodoTool` |
| `todo_list` | 查看待办 | secretary-default | `infrastructure/agent/tool/builtin/TodoTool` |
| `todo_done` | 完成待办 | secretary-default | `infrastructure/agent/tool/builtin/TodoTool` |
| `note_search` | 搜索笔记 | secretary-default | `infrastructure/agent/tool/builtin/NoteSearchTool` |
| `note_get` | 获取笔记详情 | secretary-default | `infrastructure/agent/tool/builtin/NoteSearchTool` |
| `calculator` | 计算器 | 通用 | `infrastructure/agent/tool/builtin/CalculatorTool` |
| `getLastDocStructure` | 查询文档结构 | 通用 | `infrastructure/agent/tool/DocStructureTool` |
| `saveDocStructure` | 保存文档结构 | 通用 | `infrastructure/agent/tool/DocStructureTool` |

### 注册机制

- **Tool**：在方法上标注 `@Tool(name, description, tags)`，启动时 `ToolScanner` 自动扫描注册
- **Agent**：在 Initializer 类中通过 `AgentDefinitionRepository.save()` 写入数据库，幂等设计
- **智能体（Graph）**：在 Initializer 类上标注 `@AgentGroup(id, name, description)` 并实现 `AgentGroupProvider`，启动时 `AgentGroupScanner`（@Order 200）自动创建 Graph 并绑定 Agent 节点
- **角色-工具映射**：在对应的 ToolRegistry 类中维护（如 `DevPlanToolRegistry.ROLE_TOOL_MAPPING`）

## Git 提交规范

```
feat / fix / docs / refactor / test / chore
```
