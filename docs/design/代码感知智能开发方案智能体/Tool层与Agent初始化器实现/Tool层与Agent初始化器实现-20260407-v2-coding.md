# 编码摘要文档

> 本文档由完整设计文档精简而来，供 AI 辅助编码时使用。
> 对应完整文档：`Tool层与Agent初始化器实现-20260407-v2.md`
> 父级设计文档：`整体方案设计-20260406-v2-coding.md`

---

## 变更记录

| 版本 | 日期       | 变更内容摘要 |
|------|------------|--------------|
| v1   | 2026-04-07 | 初始版本 |
| v2   | 2026-04-07 | 重构：Tool 只做机械提取，新增 DependencyAnalysisTool/CodeStructureAnalysisTool/ConfigScanTool，ArchTopologyTool 合并进 CodeStructureAnalysisTool |
| v2.1 | 2026-04-08 | CODE_AWARENESS Agent 设计拆分到 `代码感知智能体实现/`，本文档聚焦 Tool/Trace/Initializer |

---

## 1. 核心设计原则

**Tool = 眼睛（机械提取），Agent = 大脑（LLM 理解）。** Tool 绝不做判断和总结。

---

## 2. 核心业务规则

- R1：**Tool 只做机械提取，不调 LLM，不做理解判断**
- R2：Tool 命名 `devplan_` 前缀
- R3：索引粒度 — 类级别，embedding = Javadoc + 类声明 + public 方法签名
- R4：索引去重 — 同 projectPath 基于 file_hash 判断，`devplan_{projectPathHash}` collection
- R5：角色工具隔离：
  - CODE_AWARENESS → {project_scan, dependency_analysis, code_structure, config_scan, code_index}
  - REQUIREMENT_ANALYZER → {code_search, file_read}
  - SOLUTION_ARCHITECT → {code_search, template_render}
  - PLAN_REVIEWER → {}
- R6：向量库降级 — Qdrant 不可用→空结果+WARN，不阻断
- R7：Trace 全覆盖 — 每个 Node/Agent/Tool 调用生成 Span
- R8：FileReadTool — 路径必须在项目目录下
- R9：ConfigScanTool — password/secret/key 脱敏为 ***
- R10：Agent 初始化幂等 — existsById 已存在不覆盖
- R11：ToolScanner 先于 DevPlanAgentInitializer 执行

---

## 3. 接口契约

### 关键方法签名（全类名）

```java
// ===== 元数据提取器 + 向量索引器 =====

// ProjectScanTool — 目录结构、模块列表、文件统计
@Tool(name = "devplan_project_scan", description = "扫描项目目录结构和Maven模块列表")
com.exceptioncoder.llm.infrastructure.devplan.tool.ProjectScanTool#scan(
    @ToolParam("projectPath") String projectPath
): String  // JSON: {projectName, buildTool, modules[], packageTree, stats}

// DependencyAnalysisTool — pom.xml 解析、依赖清单、版本号
@Tool(name = "devplan_dependency_analysis", description = "解析pom.xml提取依赖清单和版本信息")
com.exceptioncoder.llm.infrastructure.devplan.tool.DependencyAnalysisTool#analyze(
    @ToolParam("projectPath") String projectPath
): String  // JSON: {parent, properties, dependencies[], moduleDependencies}

// CodeStructureAnalysisTool — 注解扫描、类签名、import 分析、层依赖
@Tool(name = "devplan_code_structure", description = "扫描Java注解提取Controller/Entity/Service清单和层间依赖")
com.exceptioncoder.llm.infrastructure.devplan.tool.CodeStructureAnalysisTool#analyze(
    @ToolParam("projectPath") String projectPath
): String  // JSON: {controllers[], entities[], services[], repositories[], layerDependencies, layerViolations[]}

// ConfigScanTool — 配置文件提取（脱敏）
@Tool(name = "devplan_config_scan", description = "读取application配置文件提取关键配置项")
com.exceptioncoder.llm.infrastructure.devplan.tool.ConfigScanTool#scan(
    @ToolParam("projectPath") String projectPath
): String  // JSON: {profiles[], configFiles[], server, datasource, externalServices[], customProperties}

// CodeIndexTool — 向量索引（供后续 CodeSearchTool 使用）
@Tool(name = "devplan_code_index", description = "将项目Java源文件向量化索引到Qdrant")
com.exceptioncoder.llm.infrastructure.devplan.tool.CodeIndexTool#indexIfNeeded(
    @ToolParam("projectPath") String projectPath,
    @ToolParam(value = "forceReindex", required = false, defaultValue = "false") String forceReindex
): String  // JSON: {collectionName, docCount, status, skipped}

// ===== 检索 / 读取 / 渲染工具 =====

// CodeSearchTool — 语义搜索
@Tool(name = "devplan_code_search", description = "语义搜索已索引的代码库")
com.exceptioncoder.llm.infrastructure.devplan.tool.CodeSearchTool#search(
    @ToolParam("query") String query,
    @ToolParam(value = "topK", required = false, defaultValue = "5") String topK
): String  // JSON: [{className, filePath, score, snippet}, ...]

// FileReadTool — 文件读取
@Tool(name = "devplan_file_read", description = "读取指定文件内容")
com.exceptioncoder.llm.infrastructure.devplan.tool.FileReadTool#readFile(
    @ToolParam("filePath") String filePath,
    @ToolParam(value = "maxLines", required = false, defaultValue = "200") String maxLines
): String  // 文件内容文本

// TemplateRenderTool — 模板渲染
@Tool(name = "devplan_template_render", description = "使用预定义模板渲染设计文档")
com.exceptioncoder.llm.infrastructure.devplan.tool.TemplateRenderTool#render(
    @ToolParam("templateName") String templateName,
    @ToolParam("context") String context
): String  // 渲染后 Markdown

// ===== DevPlanToolRegistry =====
com.exceptioncoder.llm.infrastructure.devplan.tool.DevPlanToolRegistry#getToolsForRole(AgentRole role): List<ToolDefinition>
com.exceptioncoder.llm.infrastructure.devplan.tool.DevPlanToolRegistry#validateAndExecute(String toolName, Map<String,Object> params, AgentRole callerRole): Object

// ===== DevPlanAgentInitializer =====
com.exceptioncoder.llm.infrastructure.devplan.config.DevPlanAgentInitializer#onApplicationEvent(ApplicationReadyEvent): void

// ===== Trace =====
com.exceptioncoder.llm.infrastructure.devplan.trace.DevPlanTraceRecorder#createTrace(): String
com.exceptioncoder.llm.infrastructure.devplan.trace.DevPlanTraceRecorder#startSpan(String name, Map<String,String> attributes): SpanContext
com.exceptioncoder.llm.infrastructure.devplan.trace.DevPlanTraceRecorder#endSpan(SpanContext span): void
com.exceptioncoder.llm.infrastructure.devplan.trace.DevPlanTraceRecorder#getSpans(String traceId): List<SpanContext>
```

---

## 4. 涉及类清单（全类名）

| 全类名 | 操作 | 说明 |
|--------|------|------|
| **Tool — 元数据提取器 + 向量索引器** | | |
| `c.e.l.infrastructure.devplan.tool.ProjectScanTool` | 新建 | @Tool @Component, 目录结构+模块+统计 |
| `c.e.l.infrastructure.devplan.tool.DependencyAnalysisTool` | 新建 | @Tool @Component, pom.xml→依赖清单 |
| `c.e.l.infrastructure.devplan.tool.CodeStructureAnalysisTool` | 新建 | @Tool @Component, 注解扫描+层依赖+违规检测 |
| `c.e.l.infrastructure.devplan.tool.ConfigScanTool` | 新建 | @Tool @Component, 配置提取+脱敏 |
| `c.e.l.infrastructure.devplan.tool.CodeIndexTool` | 新建 | @Tool @Component, Qdrant 向量索引 |
| **Tool — 检索 / 读取 / 渲染** | | |
| `c.e.l.infrastructure.devplan.tool.CodeSearchTool` | 新建 | @Tool @Component, VectorStore 语义搜索 |
| `c.e.l.infrastructure.devplan.tool.FileReadTool` | 新建 | @Tool @Component, 安全文件读取 |
| `c.e.l.infrastructure.devplan.tool.TemplateRenderTool` | 新建 | @Tool @Component, 模板渲染 |
| **标准协议** | | |
| `c.e.l.infrastructure.devplan.tool.DevPlanToolRegistry` | 新建 | @Component, 角色→工具映射+Schema 校验 |
| **Agent 初始化** | | |
| `c.e.l.infrastructure.devplan.config.DevPlanAgentInitializer` | 新建 | @Component ApplicationListener |
| **Trace** | | |
| `c.e.l.infrastructure.devplan.trace.SpanContext` | 新建 | Record(traceId, spanId, parentSpanId, name, startTimeMs, attributes) |
| `c.e.l.infrastructure.devplan.trace.DevPlanTraceRecorder` | 新建 | @Component, ThreadLocal Span 栈+日志 |

> `c.e.l` = `com.exceptioncoder.llm`

**复用类：**

| 全类名 | 复用方式 |
|--------|----------|
| `c.e.l.infrastructure.agent.tool.Tool` / `ToolParam` | 注解 |
| `c.e.l.infrastructure.agent.tool.ToolScanner` | 启动自动发现 |
| `c.e.l.infrastructure.agent.tool.ToolRegistryImpl` | 内存注册表 |
| `c.e.l.infrastructure.agent.tool.ToolExecutor` | 反射执行 |
| `c.e.l.domain.repository.AgentDefinitionRepository` | Agent 持久化 |
| `c.e.l.infrastructure.devplan.agent.DevPlanAgentConfig` | 角色→ID/Prompt |
| `org.springframework.ai.vectorstore.VectorStore` | Spring AI Qdrant |
| `c.e.l.infrastructure.devplan.repository.CodeIndexStatusRepository` | 索引状态 |
| `c.e.l.infrastructure.devplan.repository.ProjectArchTopologyRepository` | 拓扑持久化 |

---

## 5. 数据结构

### 涉及表（已存在，无新建）

```sql
-- agent_definition — DevPlanAgentInitializer 写入 4 条记录：
-- id='devplan-code-awareness',       tool_ids='["devplan_project_scan","devplan_dependency_analysis","devplan_code_structure","devplan_config_scan","devplan_code_index"]'
-- id='devplan-requirement-analyzer',  tool_ids='["devplan_code_search","devplan_file_read"]'
-- id='devplan-solution-architect',    tool_ids='["devplan_code_search","devplan_template_render"]'
-- id='devplan-plan-reviewer',         tool_ids='[]'

-- code_index_status — CodeIndexTool 读写
-- 关键字段：project_path, collection_name, doc_count, status, file_hash

-- project_arch_topology — CodeStructureAnalysisTool 写入层依赖
-- 关键字段：project_path, topology(JSON), module_list(JSON), layer_dependencies(JSON)
```

---

## 6. 重要约束与边界

- **向量存储**：直接注入 Spring AI `VectorStore`，绕过 domain 层 `VectorStoreRepository`（后者耦合 JobPosting）
- **代码分析**：正则+文件结构（不引入 JavaParser），层级识别基于包名关键词匹配
- **配置解析**：SnakeYAML（Spring 内置）+ JDK DOM XML Parser
- **Trace**：内存 ConcurrentHashMap + SLF4J 日志，ThreadLocal Span 栈
- **模板存储**：内置 Java 常量
- **无新 Maven 依赖**
- **projectPath 假设**：始终指向本地已存在的目录，clone/pull 是前置运维工作

---

## 7. 下游依赖调用

```java
// Spring AI VectorStore（Qdrant）
org.springframework.ai.vectorstore.VectorStore#add(List<Document>)                         // CodeIndexTool
org.springframework.ai.vectorstore.VectorStore#similaritySearch(String, int): List<Document> // CodeSearchTool

// JPA Repository
c.e.l.infrastructure.devplan.repository.CodeIndexStatusRepository                          // CodeIndexTool
c.e.l.infrastructure.devplan.repository.ProjectArchTopologyRepository                      // CodeStructureAnalysisTool
c.e.l.domain.repository.AgentDefinitionRepository#save(AgentDefinition)                    // Initializer
c.e.l.domain.repository.AgentDefinitionRepository#existsById(String)                       // Initializer
```

---

## 8. 异常处理要点

- Tool 异常统一返回 error JSON（如 `{"error":"路径不存在"}`），**不抛异常**（Agent 可观察并调整策略）
- Qdrant 不可用 → CodeIndexTool/CodeSearchTool 降级空结果 + WARN
- 配置文件密码等敏感字段 → `***` 脱敏
- 文件路径遍历 → FileReadTool 拒绝
- 工具越权 → DevPlanToolRegistry 抛 ToolPermissionDeniedException
- Agent 已存在 → Initializer 跳过，INFO 日志

---

## 9. 对 v2 父文档的变更影响

| v2 父文档类 | 本文档处理 |
|------------|-----------|
| ProjectScanTool | 保留，职责收窄为纯机械扫描 |
| ArchTopologyTool | **合并进** CodeStructureAnalysisTool |
| CodeIndexTool / CodeSearchTool / FileReadTool / TemplateRenderTool | 保留 |
| — | **新增** DependencyAnalysisTool, CodeStructureAnalysisTool, ConfigScanTool |

待实现验证后统一同步 v2 父文档的类清单和 CODE_AWARENESS SystemPrompt。
