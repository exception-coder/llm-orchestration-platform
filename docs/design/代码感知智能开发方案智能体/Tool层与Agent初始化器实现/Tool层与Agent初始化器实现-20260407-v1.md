# Tool 层、Trace 层与 Agent 初始化器实现设计

> 本文档是「代码感知智能开发方案智能体 v2」的**子任务实现设计**。
> 父文档：`整体方案设计-20260406-v2.md`
> 聚焦范围：devplan 模块的 Tool 标准协议层 + 全链路 Trace + Agent 启动注册

## 变更记录

| 版本 | 日期 | 修改人 | 变更内容摘要 |
|------|------|--------|--------------|
| v1 | 2026-04-07 | zhangkai | 初始版本：7 Tool + DevPlanToolRegistry + DevPlanAgentInitializer + Trace |

---

## 1. 背景与目标

### 1.1 背景

v2 整体框架已搭建完成（ScanNode → DevPlanAgentRouterImpl → AlibabaAgentExecutor 调用链），但以下关键环节为空：

1. **devplan/tool/ 目录为空** — 7 个 Tool 类（ProjectScanTool、CodeIndexTool、ArchTopologyTool、CodeSearchTool、FileReadTool、TemplateRenderTool、DevPlanToolRegistry）均未实现
2. **devplan/trace/ 目录为空** — DevPlanTraceRecorder 和 SpanContext 未实现
3. **Agent 定义未入库** — 数据库 `agent_definition` 表中无 `devplan-*` 记录，缺少类似 SecretaryAgentInitializer 的初始化器
4. **VectorStoreRepository 耦合 JobPosting** — 现有接口方法签名 `store(JobPosting, String)` 无法直接用于代码索引场景

**调用链断裂示意：**

```
ScanNode.execute()
  → agentRouter.route(CODE_AWARENESS, state)
    → agentConfig.getAgentId() → "devplan-code-awareness"
      → agentExecutor.execute()
        → agentRepository.findById("devplan-code-awareness") → ❌ 空！
          → 即使找到，agent.toolIds() 对应的 Tool → ❌ 不存在！
```

### 1.2 目标

1. 实现 6 个 `@Tool` 标注的工具类，让 ToolScanner 启动时自动发现并注册到 ToolRegistryImpl
2. 实现 DevPlanToolRegistry，提供角色→工具映射和 Schema 校验
3. 实现 DevPlanAgentInitializer，启动时将 4 个角色 Agent 写入 `agent_definition` 表
4. 实现 Trace 层，为每个 Node/Agent/Tool 调用记录 Span
5. **让 ScanNode 的完整调用链跑通**

### 1.3 设计边界

**本次包含：**
- 6 个 @Tool 实现 + DevPlanToolRegistry
- DevPlanAgentInitializer
- DevPlanTraceRecorder + SpanContext

**本次不包含：**
- VectorStoreRepository 接口泛化（使用 Spring AI VectorStore 直接操作 Qdrant）
- JavaParser AST 深度分析（ArchTopologyTool 采用轻量正则+文件结构方案，后续可增强）
- Sensor 传感器链实现（独立子任务）
- 项目仓库管理（GitLab 仓库注册、定期 clone/pull 同步）— 属于独立的前置模块

**关于 projectPath 的设计假设：**
- `projectPath` 始终指向本地已存在的项目目录（如 `/data/repos/llm-orchestration-platform`）
- 代码仓库的 clone/pull 同步是前置运维工作，不在方案生成调用链中触发
- 后续由独立的「项目管理模块」负责：注册项目 → 关联 GitLab 仓库 → 定期同步到本地工作目录

---

## 2. 技术方案

### 2.1 关键技术决策

| 决策点 | 方案 | 原因 |
|--------|------|------|
| 代码向量化存储 | 直接使用 Spring AI `VectorStore`，绕过 domain 层 `VectorStoreRepository` | 现有接口耦合 JobPosting，改造影响面大；devplan 模块通过 Infra 层直接操作 Qdrant 是合理的 |
| 架构拓扑分析 | 正则+文件结构分析（不引入 JavaParser） | 减少依赖引入复杂度；当前只需提取包层级、类声明、import 关系；后续可渐进增强为 AST |
| Qdrant 集合管理 | 每个 projectPath 对应一个 collection，命名 `devplan_{hash}` | 遵循 v2 规则5（索引去重），collection 按项目隔离 |
| Trace 实现 | 内存记录 + 日志输出（不引入 OpenTelemetry） | 一期轻量实现，满足可追踪需求；后续可接入 OTel |
| Tool 命名前缀 | `devplan_` 前缀（如 `devplan_project_scan`） | 与现有 `schedule_*`、`todo_*` 前缀保持一致，便于 Initializer 过滤 |

### 2.2 新增依赖

无新增 Maven 依赖。所有实现基于现有依赖：
- Spring AI Qdrant：`spring-ai-qdrant-store-spring-boot-starter`（已有）
- 文件操作：JDK NIO（`java.nio.file`）
- 正则分析：JDK `java.util.regex`

---

## 3. 详细设计

### 3.1 Tool 实现

#### 3.1.1 ProjectScanTool

**职责：** 扫描目标项目的目录结构和 Maven 模块列表。

**Tool ID：** `devplan_project_scan`

**输入参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectPath | String | 是 | 项目根目录绝对路径 |

**输出：** JSON 格式的 ProjectStructure，包含：
- `modules`: Maven 子模块列表（扫描 pom.xml 中的 `<modules>`）
- `sourceRoots`: 各模块 src/main/java 路径
- `packageTree`: 包目录树（深度限制 5 层）
- `javaFileCount`: Java 源文件数量

**实现逻辑：**

```
1. 验证 projectPath 存在且为目录
2. 检测是否为 Maven 项目（根目录有 pom.xml）
3. 解析根 pom.xml，提取 <modules> 列表
4. 对每个模块，扫描 src/main/java 下的 .java 文件
5. 构建包目录树（按 package 结构聚合）
6. 统计 Java 文件数量
7. 返回 ProjectStructure JSON
```

**异常处理：**
- 路径不存在 → 返回错误 JSON `{"error": "路径不存在: {path}"}`
- 无 pom.xml → 返回错误 JSON `{"error": "非 Maven 项目"}`
- 无 Java 源文件 → 返回警告 + 空结构

---

#### 3.1.2 CodeIndexTool

**职责：** 将项目 Java 源文件按类级别索引到 Qdrant 向量库。

**Tool ID：** `devplan_code_index`

**输入参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectPath | String | 是 | 项目根目录 |
| forceReindex | String | 否 | 是否强制重建索引，默认 "false" |

**输出：** JSON 格式的 CodeIndexStatus，包含 collectionName、docCount、status

**实现逻辑：**

```
1. 计算项目文件 hash（所有 .java 文件路径+大小的 MD5）
2. 查询 code_index_status 表，比对 file_hash
   - 一致且非强制 → 返回 READY，跳过索引
3. 扫描所有 .java 文件
4. 对每个文件提取类级别摘要：
   - Javadoc 注释（类级别）
   - 类声明行（class/interface/enum + extends/implements）
   - public 方法签名列表
5. 拼接为 embedding 文本："{Javadoc}\n{类声明}\n{方法签名列表}"
6. 创建 Spring AI Document（metadata: filePath, className, packageName）
7. 批量写入 VectorStore（collection: devplan_{projectPathHash}）
8. 更新 code_index_status 表
```

**关键依赖：**
- `org.springframework.ai.vectorstore.VectorStore` — 直接注入，由 Spring AI Qdrant 自动配置提供
- `CodeIndexStatusRepository` — JPA，查询/更新索引状态

**向量化粒度（遵循 v2 规则6）：** 类级别，embedding = Javadoc + 类声明 + public 方法签名

---

#### 3.1.3 ArchTopologyTool

**职责：** 分析项目代码的分层架构拓扑（DDD-lite 层次和模块间依赖）。

**Tool ID：** `devplan_arch_topology`

**输入参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectPath | String | 是 | 项目根目录 |

**输出：** JSON 格式的 ArchTopology，包含：
- `layers`: 分层识别结果（api/application/domain/infrastructure）
- `modules`: 模块列表及各模块的层归属
- `dependencies`: 层间依赖关系（import 分析）
- `violations`: 违规依赖（如 domain → infrastructure）

**实现逻辑：**

```
1. 扫描所有 .java 文件
2. 对每个文件：
   a. 读取 package 声明 → 归类到 layer（按包名关键词匹配）
      - 含 "controller" / "api" / "web" → API 层
      - 含 "application" / "usecase" / "service"(非 domain 下) → Application 层
      - 含 "domain" / "model" / "entity" → Domain 层
      - 含 "infrastructure" / "repository" / "config" → Infrastructure 层
   b. 提取 import 语句 → 分析依赖方向
3. 聚合层间依赖矩阵
4. 检测违规：domain 层 import 了 infrastructure 包 → 标记违规
5. 构建 ArchTopology JSON
6. 持久化到 project_arch_topology 表（UPSERT）
```

---

#### 3.1.4 CodeSearchTool

**职责：** 基于语义搜索在已索引的代码库中检索相关类。

**Tool ID：** `devplan_code_search`

**输入参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| query | String | 是 | 搜索查询文本 |
| topK | String | 否 | 返回结果数量，默认 "5" |

**输出：** JSON 数组，每项包含 className、filePath、score、snippet

**实现逻辑：**

```
1. 使用 VectorStore.similaritySearch(query, topK) 检索
2. 从返回的 Document 中提取 metadata（className, filePath, packageName）
3. 对每个命中结果，读取对应文件的类声明和 public 方法签名作为 snippet
4. 组装结果 JSON 返回
```

**降级策略（v2 规则）：** VectorStore 不可用 → 返回空结果 + WARN 日志，不阻断流程

---

#### 3.1.5 FileReadTool

**职责：** 读取指定文件的内容。

**Tool ID：** `devplan_file_read`

**输入参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| filePath | String | 是 | 文件绝对路径 |
| maxLines | String | 否 | 最大行数，默认 "200" |

**输出：** 文件内容文本（截断到 maxLines）

**实现逻辑：**

```
1. 验证文件存在且为普通文件
2. 安全检查：文件路径必须在项目目录下（防止路径遍历）
3. 读取文件内容，截断到 maxLines 行
4. 返回文件内容字符串
```

---

#### 3.1.6 TemplateRenderTool

**职责：** 使用预定义模板渲染设计文档。

**Tool ID：** `devplan_template_render`

**输入参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| templateName | String | 是 | 模板名称（如 "STANDARD"） |
| context | String | 是 | JSON 格式的模板上下文变量 |

**输出：** 渲染后的 Markdown 文本

**实现逻辑：**

```
1. 根据 templateName 选择内置模板
   - STANDARD：标准设计文档模板（含所有章节）
   - LIGHTWEIGHT：轻量模板（仅核心章节）
2. 解析 context JSON 为 Map
3. 对模板中的 {variable} 占位符进行替换
4. 返回渲染后的 Markdown
```

**模板存储：** 内置 Java 常量（后续可迁移到数据库 prompt_template 表）

---

### 3.2 DevPlanToolRegistry

**职责：** Tool 标准协议层 — 按角色路由工具集 + JSON Schema 参数校验。

**类名：** `com.exceptioncoder.llm.infrastructure.devplan.tool.DevPlanToolRegistry`

**设计：**

```java
@Component
public class DevPlanToolRegistry {

    // 角色 → 工具 ID 列表的静态映射
    private static final Map<AgentRole, List<String>> ROLE_TOOL_MAPPING = Map.of(
        CODE_AWARENESS, List.of("devplan_project_scan", "devplan_code_index", "devplan_arch_topology"),
        REQUIREMENT_ANALYZER, List.of("devplan_code_search", "devplan_file_read"),
        SOLUTION_ARCHITECT, List.of("devplan_code_search", "devplan_template_render"),
        PLAN_REVIEWER, List.of()  // 无外部工具
    );

    // 获取角色对应的工具列表
    List<ToolDefinition> getToolsForRole(AgentRole role);

    // 校验参数并执行工具
    Object validateAndExecute(String toolName, Map<String, Object> params);
}
```

**Schema 校验逻辑：**
1. 从 ToolRegistryImpl 获取 ToolDefinition
2. 解析 inputSchema（JSON Schema）
3. 校验 params 中的 required 字段是否存在、类型是否匹配
4. 校验通过 → 委托 ToolExecutor 执行
5. 校验失败 → 抛出 ToolSchemaValidationException

**角色权限校验（v2 规则3）：**
- 调用 validateAndExecute 时传入 AgentRole
- 检查该工具是否在角色的工具集中
- 不在 → 抛出 ToolPermissionDeniedException

---

### 3.3 DevPlanAgentInitializer

**职责：** 应用启动时将 4 个 devplan 角色 Agent 写入数据库。

**类名：** `com.exceptioncoder.llm.infrastructure.devplan.config.DevPlanAgentInitializer`

**实现逻辑：**

```
监听 ApplicationReadyEvent（@Order 确保在 ToolScanner 之后执行）

for each AgentRole:
    1. agentId = DevPlanAgentConfig.getAgentId(role)
    2. if agentRepository.existsById(agentId) → 跳过
    3. toolIds = DevPlanToolRegistry.ROLE_TOOL_MAPPING.get(role)
    4. systemPrompt = DevPlanAgentConfig.getSystemPrompt(role)
    5. 构建 AgentDefinition(id, name, description, systemPrompt, toolIds, ...)
    6. agentRepository.save(agentDefinition)
```

**关键点：**
- 幂等：existsById 检查，已存在不覆盖
- 顺序：ToolScanner 先注册 Tool → DevPlanAgentInitializer 再绑定 Tool ID
- LLM 配置：使用默认模型（由 LLMConfiguration 管理），不在 Agent 定义中硬编码

---

### 3.4 Trace 层

#### 3.4.1 SpanContext

```java
public record SpanContext(
    String traceId,      // 整个任务的 Trace ID
    String spanId,       // 当前 Span ID
    String parentSpanId, // 父 Span ID（可空）
    String name,         // Span 名称（如 "ScanNode", "devplan_project_scan"）
    long startTimeMs,    // 开始时间戳
    Map<String, String> attributes  // 附加属性
)
```

#### 3.4.2 DevPlanTraceRecorder

**职责：** 管理 Trace 生命周期，记录 Span 层级关系。

```java
@Component
public class DevPlanTraceRecorder {

    // 基于 ThreadLocal 的 Span 栈
    private final ThreadLocal<Deque<SpanContext>> spanStack;

    // 生成 Trace ID（任务级别）
    String createTrace();

    // 开始一个 Span（自动关联父 Span）
    SpanContext startSpan(String name, Map<String, String> attributes);

    // 结束 Span 并记录耗时
    void endSpan(SpanContext span);

    // 获取当前任务的所有 Span（用于结果输出）
    List<SpanContext> getSpans(String traceId);
}
```

**记录方式：**
- 一期：内存 ConcurrentHashMap + SLF4J 结构化日志输出
- 日志格式：`[TRACE] traceId={} spanId={} parentSpanId={} name={} elapsedMs={}`
- 后续可接入 OpenTelemetry

---

## 4. 类清单（全类名）

| 全类名 | 操作 | 说明 |
|--------|------|------|
| `com.exceptioncoder.llm.infrastructure.devplan.tool.ProjectScanTool` | 新建 | 项目结构扫描 @Tool |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.CodeIndexTool` | 新建 | 代码向量索引 @Tool |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.ArchTopologyTool` | 新建 | 架构拓扑分析 @Tool |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.CodeSearchTool` | 新建 | 代码语义搜索 @Tool |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.FileReadTool` | 新建 | 文件内容读取 @Tool |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.TemplateRenderTool` | 新建 | 模板渲染 @Tool |
| `com.exceptioncoder.llm.infrastructure.devplan.tool.DevPlanToolRegistry` | 新建 | 角色工具路由 + Schema 校验 |
| `com.exceptioncoder.llm.infrastructure.devplan.config.DevPlanAgentInitializer` | 新建 | Agent 启动注册 |
| `com.exceptioncoder.llm.infrastructure.devplan.trace.SpanContext` | 新建 | Span 上下文 Record |
| `com.exceptioncoder.llm.infrastructure.devplan.trace.DevPlanTraceRecorder` | 新建 | Trace 记录器 |

**复用类：**

| 全类名 | 复用方式 |
|--------|----------|
| `com.exceptioncoder.llm.infrastructure.agent.tool.Tool` | @Tool 注解 |
| `com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam` | @ToolParam 注解 |
| `com.exceptioncoder.llm.infrastructure.agent.tool.ToolScanner` | 自动发现 @Tool |
| `com.exceptioncoder.llm.infrastructure.agent.tool.ToolRegistryImpl` | 内存注册表 |
| `com.exceptioncoder.llm.infrastructure.agent.tool.ToolExecutor` | 反射执行工具 |
| `com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository` | Agent 持久化 |
| `com.exceptioncoder.llm.infrastructure.devplan.agent.DevPlanAgentConfig` | Agent 配置映射 |
| `org.springframework.ai.vectorstore.VectorStore` | Spring AI 向量存储 |

---

## 5. 核心业务规则

| # | 规则 | 来源 |
|---|------|------|
| R1 | Tool 命名：`devplan_` 前缀 | 本文档 |
| R2 | 索引粒度：类级别，embedding = Javadoc + 类声明 + public 方法签名 | v2 规则6 |
| R3 | 索引去重：同 projectPath 只维护一个 collection，基于 file_hash 判断重建 | v2 规则5 |
| R4 | 角色工具隔离：每个 Agent 只能调用其角色对应的工具集 | v2 规则3 |
| R5 | Schema 校验：所有 @Tool 调用前通过 JSON Schema 参数校验 | v2 规则4 |
| R6 | 向量库降级：不可用时纯 LLM 分析，标注降级 | v2 约束 |
| R7 | Trace 全覆盖：每个 Node/LLM/Tool 调用生成 Span | v2 规则14 |
| R8 | FileReadTool 安全检查：路径必须在项目目录下 | 本文档 |
| R9 | Agent 初始化幂等：existsById 检查，已存在不覆盖 | 本文档 |
| R10 | Initializer 顺序：ToolScanner 先于 AgentInitializer | 本文档 |

---

## 6. 异常处理

| 场景 | 处理方式 |
|------|----------|
| projectPath 不存在 | Tool 返回 error JSON，不抛异常（Agent 可观察到错误并调整） |
| 非 Maven 项目 | Tool 返回 error JSON |
| Qdrant 不可用 | CodeIndexTool/CodeSearchTool 降级返回空结果 + WARN 日志 |
| 文件路径遍历攻击 | FileReadTool 拒绝非项目目录路径 |
| Tool 不在角色权限内 | DevPlanToolRegistry 抛出 ToolPermissionDeniedException |
| Schema 校验失败 | DevPlanToolRegistry 返回校验错误信息 |
| Agent 定义已存在 | DevPlanAgentInitializer 跳过，INFO 日志 |

---

## 7. 测试要点

| 测试项 | 类型 | 说明 |
|--------|------|------|
| ProjectScanTool 扫描 Maven 多模块项目 | 单元测试 | 使用临时目录构造测试项目 |
| CodeIndexTool 索引去重 | 单元测试 | mock VectorStore，验证 file_hash 比对逻辑 |
| ArchTopologyTool 层级识别 | 单元测试 | 验证 package 名到 layer 的映射 |
| DevPlanToolRegistry 角色路由 | 单元测试 | 验证每个角色返回正确的工具集 |
| DevPlanToolRegistry 权限拒绝 | 单元测试 | PLAN_REVIEWER 尝试调用 ProjectScanTool → 拒绝 |
| DevPlanAgentInitializer 幂等 | 集成测试 | 两次启动只创建一次 |
| ScanNode 全链路 | 集成测试 | 端到端验证调用链完整性 |
