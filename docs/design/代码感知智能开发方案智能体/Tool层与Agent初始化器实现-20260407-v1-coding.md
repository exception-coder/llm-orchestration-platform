# 编码摘要文档

> 本文档由完整设计文档精简而来，供 AI 辅助编码时使用。
> 对应完整文档：`Tool层与Agent初始化器实现-20260407-v1.md`
> 父级设计文档：`代码感知智能开发方案智能体-20260406-v2-coding.md`

---

## 变更记录

| 版本 | 日期 | 变更内容摘要 |
|------|------|--------------|
| v1 | 2026-04-07 | 初始版本：6 Tool + DevPlanToolRegistry + DevPlanAgentInitializer + Trace |

---

## 1. 核心业务规则

- R1：Tool 命名 — `devplan_` 前缀（如 `devplan_project_scan`）
- R2：索引粒度 — 类级别，embedding = Javadoc + 类声明 + public 方法签名
- R3：索引去重 — 同 projectPath 只维护一个 collection（`devplan_{projectPathHash}`），基于 file_hash 判断重建
- R4：角色工具隔离 — CODE_AWARENESS→{project_scan, code_index, arch_topology}，REQUIREMENT_ANALYZER→{code_search, file_read}，SOLUTION_ARCHITECT→{code_search, template_render}，PLAN_REVIEWER→{}
- R5：Schema 校验 — @Tool 调用前通过 JSON Schema 参数校验
- R6：向量库降级 — Qdrant 不可用→返回空结果+WARN 日志，不阻断流程
- R7：Trace 全覆盖 — 每个 Node/LLM/Tool 调用生成 Span
- R8：FileReadTool 安全 — 路径必须在项目目录下，防止路径遍历
- R9：Agent 初始化幂等 — existsById 检查，已存在不覆盖
- R10：Initializer 顺序 — ToolScanner 先于 DevPlanAgentInitializer 执行

---

## 2. 接口契约

### 关键方法签名（全类名）

```java
// ===== Tool 层 =====

// ProjectScanTool — @Tool("devplan_project_scan")
com.exceptioncoder.llm.infrastructure.devplan.tool.ProjectScanTool#scan(
    @ToolParam("projectPath") String projectPath
): String  // 返回 ProjectStructure JSON

// CodeIndexTool — @Tool("devplan_code_index")
com.exceptioncoder.llm.infrastructure.devplan.tool.CodeIndexTool#indexIfNeeded(
    @ToolParam("projectPath") String projectPath,
    @ToolParam("forceReindex") String forceReindex  // "true"/"false"，默认 "false"
): String  // 返回 CodeIndexStatus JSON

// ArchTopologyTool — @Tool("devplan_arch_topology")
com.exceptioncoder.llm.infrastructure.devplan.tool.ArchTopologyTool#extractTopology(
    @ToolParam("projectPath") String projectPath
): String  // 返回 ArchTopology JSON

// CodeSearchTool — @Tool("devplan_code_search")
com.exceptioncoder.llm.infrastructure.devplan.tool.CodeSearchTool#search(
    @ToolParam("query") String query,
    @ToolParam("topK") String topK  // 默认 "5"
): String  // 返回 RelevantCode JSON 数组

// FileReadTool — @Tool("devplan_file_read")
com.exceptioncoder.llm.infrastructure.devplan.tool.FileReadTool#readFile(
    @ToolParam("filePath") String filePath,
    @ToolParam("maxLines") String maxLines  // 默认 "200"
): String  // 返回文件内容

// TemplateRenderTool — @Tool("devplan_template_render")
com.exceptioncoder.llm.infrastructure.devplan.tool.TemplateRenderTool#render(
    @ToolParam("templateName") String templateName,
    @ToolParam("context") String context  // JSON 格式上下文
): String  // 返回渲染后 Markdown

// ===== DevPlanToolRegistry =====
com.exceptioncoder.llm.infrastructure.devplan.tool.DevPlanToolRegistry#getToolsForRole(AgentRole role): List<ToolDefinition>
com.exceptioncoder.llm.infrastructure.devplan.tool.DevPlanToolRegistry#validateAndExecute(String toolName, Map<String,Object> params, AgentRole callerRole): Object

// ===== DevPlanAgentInitializer =====
com.exceptioncoder.llm.infrastructure.devplan.config.DevPlanAgentInitializer#onApplicationEvent(ApplicationReadyEvent): void

// ===== Trace 层 =====
com.exceptioncoder.llm.infrastructure.devplan.trace.DevPlanTraceRecorder#createTrace(): String
com.exceptioncoder.llm.infrastructure.devplan.trace.DevPlanTraceRecorder#startSpan(String name, Map<String,String> attributes): SpanContext
com.exceptioncoder.llm.infrastructure.devplan.trace.DevPlanTraceRecorder#endSpan(SpanContext span): void
com.exceptioncoder.llm.infrastructure.devplan.trace.DevPlanTraceRecorder#getSpans(String traceId): List<SpanContext>
```

---

## 3. 涉及类清单（全类名）

| 全类名 | 操作 | 说明 |
|--------|------|------|
| **Tool 层** | | |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.ProjectScanTool` | 新建 | @Tool, @Component, 扫描 Maven 项目结构 |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.CodeIndexTool` | 新建 | @Tool, @Component, 代码向量索引到 Qdrant |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.ArchTopologyTool` | 新建 | @Tool, @Component, 正则+文件结构分析架构拓扑 |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.CodeSearchTool` | 新建 | @Tool, @Component, VectorStore 语义搜索 |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.FileReadTool` | 新建 | @Tool, @Component, 安全文件读取 |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.TemplateRenderTool` | 新建 | @Tool, @Component, 设计文档模板渲染 |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.DevPlanToolRegistry` | 新建 | @Component, 角色→工具映射 + Schema 校验 |
| **Agent 初始化** | | |
| `com.exceptioncoder.llm.infrastructure.devplan.config.DevPlanAgentInitializer` | 新建 | @Component, ApplicationListener, 启动时注册 4 个 Agent |
| **Trace 层** | | |
| `com.exceptioncoder.llm.infrastructure.devplan.trace.SpanContext` | 新建 | Record: traceId, spanId, parentSpanId, name, startTimeMs, attributes |
| `com.exceptioncoder.llm.infrastructure.devplan.trace.DevPlanTraceRecorder` | 新建 | @Component, ThreadLocal Span 栈 + 日志输出 |
| **复用类** | | |
| `com.exceptioncoder.llm.infrastructure.agent.tool.Tool` | 复用 | @Tool 注解 |
| `com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam` | 复用 | @ToolParam 注解 |
| `com.exceptioncoder.llm.infrastructure.agent.tool.ToolScanner` | 复用 | 启动时自动发现 @Tool 方法 |
| `com.exceptioncoder.llm.infrastructure.agent.tool.ToolRegistryImpl` | 复用 | 内存工具注册表 |
| `com.exceptioncoder.llm.infrastructure.agent.tool.ToolExecutor` | 复用 | 反射执行工具 |
| `com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository` | 复用 | Agent 持久化 |
| `com.exceptioncoder.llm.infrastructure.devplan.agent.DevPlanAgentConfig` | 复用 | 角色→AgentID/Prompt 映射 |
| `org.springframework.ai.vectorstore.VectorStore` | 复用 | Spring AI 向量存储（自动配置 Qdrant） |
| `com.exceptioncoder.llm.infrastructure.devplan.repository.CodeIndexStatusRepository` | 复用 | 索引状态 JPA |
| `com.exceptioncoder.llm.infrastructure.devplan.repository.ProjectArchTopologyRepository` | 复用 | 架构拓扑 JPA |

---

## 4. 数据结构

### 涉及表（已存在，无新建表）

```sql
-- code_index_status（v2 已建）
-- 关键字段：project_path, collection_name, doc_count, status, file_hash

-- project_arch_topology（v2 已建）
-- 关键字段：project_path, topology(JSON), module_list(JSON), layer_dependencies(JSON)

-- agent_definition（平台级已建）
-- DevPlanAgentInitializer 写入 4 条记录：
--   id = 'devplan-code-awareness', tool_ids = '["devplan_project_scan","devplan_code_index","devplan_arch_topology"]'
--   id = 'devplan-requirement-analyzer', tool_ids = '["devplan_code_search","devplan_file_read"]'
--   id = 'devplan-solution-architect', tool_ids = '["devplan_code_search","devplan_template_render"]'
--   id = 'devplan-plan-reviewer', tool_ids = '[]'
```

### Tool 输出 JSON 结构

```json
// ProjectScanTool 输出
{
  "modules": ["llm-domain", "llm-application", "llm-infrastructure", "llm-api"],
  "sourceRoots": {"llm-domain": "llm-domain/src/main/java", ...},
  "packageTree": {"com.exceptioncoder.llm": {"domain": {...}, "application": {...}}},
  "javaFileCount": 85
}

// CodeIndexTool 输出
{
  "collectionName": "devplan_a1b2c3d4",
  "docCount": 85,
  "status": "READY"
}

// ArchTopologyTool 输出
{
  "layers": {
    "api": ["com.exceptioncoder.llm.api.controller.*"],
    "application": ["com.exceptioncoder.llm.application.*"],
    "domain": ["com.exceptioncoder.llm.domain.*"],
    "infrastructure": ["com.exceptioncoder.llm.infrastructure.*"]
  },
  "modules": [{"name": "llm-domain", "layer": "domain"}, ...],
  "dependencies": [{"from": "api", "to": "application"}, {"from": "application", "to": "domain"}, ...],
  "violations": []
}

// CodeSearchTool 输出
[
  {"className": "DevPlanUseCase", "filePath": "llm-application/src/.../DevPlanUseCase.java", "score": 0.87, "snippet": "..."},
  ...
]
```

---

## 5. 重要约束与边界

- **向量存储**：直接注入 Spring AI `VectorStore`（由 spring-ai-qdrant-store-spring-boot-starter 自动配置），绕过 domain 层 `VectorStoreRepository`（后者耦合 JobPosting）
- **架构分析**：正则+文件结构方案（不引入 JavaParser），层级识别基于包名关键词匹配
- **Trace 实现**：内存 ConcurrentHashMap + SLF4J 日志，ThreadLocal Span 栈管理层级
- **模板存储**：内置 Java 常量（后续可迁移 prompt_template 表）
- **无新 Maven 依赖**：所有实现基于 Spring AI Qdrant（已有）+ JDK NIO + JDK Regex

---

## 6. 下游依赖调用

```java
// Spring AI VectorStore（Qdrant 实现）
org.springframework.ai.vectorstore.VectorStore#add(List<Document>): void           // CodeIndexTool 写入
org.springframework.ai.vectorstore.VectorStore#similaritySearch(String query, int topK): List<Document>  // CodeSearchTool 检索

// JPA Repository（已存在）
com.exceptioncoder.llm.infrastructure.devplan.repository.CodeIndexStatusRepository  // CodeIndexTool 索引状态
com.exceptioncoder.llm.infrastructure.devplan.repository.ProjectArchTopologyRepository  // ArchTopologyTool 拓扑持久化
com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository#save(AgentDefinition): void  // Initializer 写入
com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository#existsById(String): boolean  // Initializer 幂等检查
```

---

## 7. 异常处理要点

- projectPath 不存在 → Tool 返回 `{"error": "路径不存在: {path}"}` 而非抛异常，Agent 可观察并调整
- Qdrant 不可用 → CodeIndexTool/CodeSearchTool 降级返回空结果 + WARN 日志
- 文件路径遍历 → FileReadTool 返回 `{"error": "路径安全检查失败"}`
- 工具越权 → DevPlanToolRegistry 抛 ToolPermissionDeniedException
- Agent 已存在 → DevPlanAgentInitializer 跳过，INFO 日志
