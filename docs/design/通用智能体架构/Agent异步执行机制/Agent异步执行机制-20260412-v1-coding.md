# 编码摘要文档

> 对应完整文档：`Agent异步执行机制-20260412-v1.md`
>
> **职责边界**：设计文档回答"哪些类、什么职责、怎么协作"，本文档回答"每个方法怎么写"。

---

## 变更记录

| 版本 | 日期 | 变更内容摘要 |
|------|------|--------------|
| v1 | 2026-04-12 | 初始版本 |

---

## 1. 核心业务规则

- R1：`POST /execute` 必须在 50ms 内响应，禁止阻塞等待 LLM 调用
- R2：同时执行的 Agent 数量不超过 `agent.async.max-concurrent`（默认 5），超限返回 HTTP 429
- R3：单次执行总时长不超过 `AgentDefinition.timeoutSeconds`（默认 120s），超时 `Future.cancel(true)`
- R4：单次 LLM 调用超时后最多重试 2 次，3 次均失败则标记当前迭代失败
- R5：连续 3 轮迭代均因 LLM 超时失败，直接终止执行，状态 → FAILED
- R6：SSE 客户端断开不影响后台 Agent 执行，结果保留在 AgentTaskStore
- R7：已完成/失败任务在 AgentTaskStore 中保留 30 分钟后自动清除
- R8：executionId 格式 `exec-{yyyyMMdd}-{自增序号}`

---

## 2. 接口契约

### 入口接口

```
POST /api/v1/agents/{agentId}/execute（改造：同步 → 异步）
请求：AgentController.AgentExecuteRequest — input(String), context(Map)
返回：AgentExecuteAsyncResponse — executionId(String), agentId(String), status(String)
HTTP 状态码：202 Accepted

GET /api/v1/agents/executions/{executionId}（新增）
返回：AgentExecutionStatusResponse — executionId, agentId, status, currentIteration, maxIterations, finalOutput, errorMessage, thoughtHistory, toolCalls, elapsedMs

GET /api/v1/agents/executions/{executionId}/stream（新增，SSE）
返回：SseEmitter，推送 iteration / tool_result / complete / error / timeout 事件
```

### 请求示例

```http
POST /api/v1/agents/devplan-requirement-analyzer/execute
Content-Type: application/json

{
  "input": "分析用户权限管理模块的需求影响范围",
  "context": {
    "projectPath": "/path/to/project",
    "requirement": "新增 RBAC 角色权限管理"
  }
}
```

---

## 3. 涉及类清单（全路径）

| 全路径 | 操作 | 说明 |
|--------|------|------|
| `com.exceptioncoder.llm.domain.model.AgentTask` | 新建 | Agent 执行任务生命周期 record |
| `com.exceptioncoder.llm.domain.model.AgentTaskManager` | 新建 | 任务生命周期管理接口 |
| `com.exceptioncoder.llm.domain.executor.AgentIterationListener` | 新建 | 迭代级事件回调接口 |
| `com.exceptioncoder.llm.domain.executor.AgentExecutor` | 改造 | 新增带 listener 参数的 execute 重载 |
| `com.exceptioncoder.llm.infrastructure.agent.task.AgentTaskManagerImpl` | 新建 | TaskManager 实现，含并发 + 超时控制 |
| `com.exceptioncoder.llm.infrastructure.agent.task.AgentTaskStore` | 新建 | ConcurrentHashMap 内存任务存储 |
| `com.exceptioncoder.llm.infrastructure.agent.task.AgentSseManager` | 新建 | SSE 连接管理，实现 AgentIterationListener |
| `com.exceptioncoder.llm.infrastructure.agent.task.AgentAsyncConfig` | 新建 | 线程池 + 并发参数配置类 |
| `com.exceptioncoder.llm.infrastructure.agent.executor.AlibabaAgentExecutor` | 改造 | ReAct 循环中回调 listener，改进异常处理 |
| `com.exceptioncoder.llm.application.usecase.AgentExecutionUseCase` | 改造 | 新增 submitAsync / getExecutionStatus |
| `com.exceptioncoder.llm.api.controller.management.AgentController` | 改造 | execute 改为 202，新增 2 个端点 |

### 关键方法签名与职责

```
// ─── Domain 层：AgentTask ───
com.exceptioncoder.llm.domain.model.AgentTask
  — record(executionId, agentId, status, currentIteration, maxIterations,
           finalOutput, errorMessage, thoughtHistory, toolCalls,
           startedAt, completedAt, createdAt, elapsedMs)
  — Status enum: SUBMITTED, RUNNING, COMPLETED, FAILED, TIMED_OUT
  — Builder 模式，支持 withStatus / withCurrentIteration 等函数式状态转换

// ─── Domain 层：AgentTaskManager ───
com.exceptioncoder.llm.domain.model.AgentTaskManager#submit(String agentId, String input, Map<String,Object> context, int timeoutSeconds): AgentTask
  — 创建 SUBMITTED 状态任务，提交到线程池异步执行，返回初始任务对象
com.exceptioncoder.llm.domain.model.AgentTaskManager#getTask(String executionId): Optional<AgentTask>
  — 查询任务当前状态
com.exceptioncoder.llm.domain.model.AgentTaskManager#completeTask(String executionId, AgentExecutionResult result): void
  — 标记任务完成，填充 finalOutput / toolCalls / thoughtHistory
com.exceptioncoder.llm.domain.model.AgentTaskManager#failTask(String executionId, String errorMessage): void
  — 标记任务失败

// ─── Domain 层：AgentIterationListener ───
com.exceptioncoder.llm.domain.executor.AgentIterationListener#onIteration(String executionId, int iteration, String thought, ToolCall toolCall): void
  — 每轮 ReAct 迭代 LLM 返回后回调
com.exceptioncoder.llm.domain.executor.AgentIterationListener#onToolResult(String executionId, int iteration, String toolName, String output): void
  — 工具执行完成后回调
com.exceptioncoder.llm.domain.executor.AgentIterationListener#onComplete(String executionId, AgentExecutionResult result): void
  — 执行正常结束回调
com.exceptioncoder.llm.domain.executor.AgentIterationListener#onError(String executionId, String errorMessage): void
  — 执行异常回调

// ─── Domain 层：AgentExecutor 改造 ───
com.exceptioncoder.llm.domain.executor.AgentExecutor#execute(AgentExecutionRequest request, AgentIterationListener listener): AgentExecutionResult
  — 新增重载，支持传入 listener；原无参版本保持兼容（传 null/no-op listener）

// ─── Infrastructure 层：AgentTaskManagerImpl ───
com.exceptioncoder.llm.infrastructure.agent.task.AgentTaskManagerImpl#submit(String agentId, String input, Map context, int timeoutSeconds): AgentTask
  — 1. Semaphore.tryAcquire() 检查并发
  — 2. 生成 executionId（exec-yyyyMMdd-NNN）
  — 3. 创建 AgentTask(SUBMITTED) 存入 AgentTaskStore
  — 4. threadPool.submit(Runnable) 提交异步执行
  — 5. Runnable 内部：构建 AgentExecutionRequest → executor.execute(request, listener) → completeTask/failTask → semaphore.release()
  — 6. 整体超时：Future.get(timeoutSeconds, SECONDS)，超时 future.cancel(true) + failTask(TIMED_OUT)
  — 7. 返回初始 AgentTask

// ─── Infrastructure 层：AgentTaskStore ───
com.exceptioncoder.llm.infrastructure.agent.task.AgentTaskStore#save(AgentTask task): void
  — ConcurrentHashMap.put(executionId, task)
com.exceptioncoder.llm.infrastructure.agent.task.AgentTaskStore#get(String executionId): Optional<AgentTask>
  — ConcurrentHashMap.get
com.exceptioncoder.llm.infrastructure.agent.task.AgentTaskStore#update(String executionId, UnaryOperator<AgentTask> updater): void
  — ConcurrentHashMap.computeIfPresent，原子更新
com.exceptioncoder.llm.infrastructure.agent.task.AgentTaskStore#cleanExpired(Duration retainDuration): void
  — @Scheduled 定时清理已完成且超过 retainDuration 的任务

// ─── Infrastructure 层：AgentSseManager ───
com.exceptioncoder.llm.infrastructure.agent.task.AgentSseManager#register(String executionId, SseEmitter emitter): void
  — 注册 SSE 连接，绑定 onCompletion / onTimeout / onError 回调自动移除
com.exceptioncoder.llm.infrastructure.agent.task.AgentSseManager#onIteration(String executionId, int iteration, String thought, ToolCall toolCall): void
  — 实现 AgentIterationListener，向对应 emitter 发送 event: iteration
com.exceptioncoder.llm.infrastructure.agent.task.AgentSseManager#onToolResult(String executionId, int iteration, String toolName, String output): void
  — 发送 event: tool_result
com.exceptioncoder.llm.infrastructure.agent.task.AgentSseManager#onComplete(String executionId, AgentExecutionResult result): void
  — 发送 event: complete + [DONE]，调用 emitter.complete()
com.exceptioncoder.llm.infrastructure.agent.task.AgentSseManager#onError(String executionId, String errorMessage): void
  — 发送 event: error，调用 emitter.completeWithError()

// ─── Infrastructure 层：AgentAsyncConfig ───
com.exceptioncoder.llm.infrastructure.agent.task.AgentAsyncConfig
  — @ConfigurationProperties(prefix = "agent.async")
  — maxConcurrent(int, default 5), threadPoolSize(int, default 5),
    executionTimeout(int, default 120), taskRetainMinutes(int, default 30),
    sseTimeout(long, default 600000)
  — @Bean ExecutorService agentExecutorPool(): Executors.newFixedThreadPool(threadPoolSize)
  — @Bean Semaphore agentConcurrencySemaphore(): new Semaphore(maxConcurrent)

// ─── Infrastructure 层：AlibabaAgentExecutor 改造 ───
com.exceptioncoder.llm.infrastructure.agent.executor.AlibabaAgentExecutor#execute(AgentExecutionRequest request, AgentIterationListener listener): AgentExecutionResult
  — while 循环中每轮迭代后调用 listener.onIteration()
  — 工具执行完成后调用 listener.onToolResult()
  — 循环结束后调用 listener.onComplete() 或 listener.onError()
  — 异常处理改进：单次 LLM 超时重试 2 次，连续 3 轮失败触发熔断
  — consecutiveFailures 计数器，达到 3 时 break 循环

// ─── Application 层：AgentExecutionUseCase 改造 ───
com.exceptioncoder.llm.application.usecase.AgentExecutionUseCase#submitAsync(String agentId, String input, Map context): AgentTask
  — 1. 校验 Agent 存在
  — 2. 获取 AgentDefinition.timeoutSeconds
  — 3. 委托 AgentTaskManager.submit()
  — 4. 返回 AgentTask（含 executionId）
com.exceptioncoder.llm.application.usecase.AgentExecutionUseCase#getExecutionStatus(String executionId): Optional<AgentTask>
  — 委托 AgentTaskManager.getTask()
  — 注意：原同步 execute() 方法保留，供内部编排调用

// ─── API 层：AgentController 改造 ───
com.exceptioncoder.llm.api.controller.management.AgentController#execute(String agentId, AgentExecuteRequest request): ResponseEntity<AgentExecuteAsyncResponse>
  — 改为返回 202 Accepted + AgentExecuteAsyncResponse(executionId, agentId, "SUBMITTED")
  — 并发超限时返回 429
com.exceptioncoder.llm.api.controller.management.AgentController#getExecutionStatus(String executionId): ResponseEntity<AgentExecutionStatusResponse>
  — GET /api/v1/agents/executions/{executionId}
  — 委托 useCase.getExecutionStatus()，不存在返回 404
com.exceptioncoder.llm.api.controller.management.AgentController#streamExecution(String executionId): SseEmitter
  — GET /api/v1/agents/executions/{executionId}/stream
  — produces = TEXT_EVENT_STREAM_VALUE
  — 创建 SseEmitter(sseTimeout) → 注册到 AgentSseManager → 返回 emitter
  — 若任务不存在，直接 emitter.completeWithError(new NotFoundException())
  — 若任务已完成，发送最终结果后 emitter.complete()
```

---

## 4. 数据结构

### 关键 DTO/Record 字段

```java
// AgentTask（Domain record，全路径：com.exceptioncoder.llm.domain.model.AgentTask）
String executionId;         // 执行唯一标识，格式 exec-yyyyMMdd-NNN
String agentId;             // Agent ID
Status status;              // SUBMITTED / RUNNING / COMPLETED / FAILED / TIMED_OUT
int currentIteration;       // 当前迭代轮次（0 表示未开始）
int maxIterations;          // 最大迭代轮次（来自 AgentDefinition）
String finalOutput;         // 最终输出（仅 COMPLETED）
String errorMessage;        // 错误信息（仅 FAILED / TIMED_OUT）
List<String> thoughtHistory;     // 思考历史
List<ToolCall> toolCalls;        // 工具调用记录
LocalDateTime startedAt;         // 开始执行时间
LocalDateTime completedAt;       // 完成时间
LocalDateTime createdAt;         // 创建时间

// AgentExecuteAsyncResponse（API DTO，Controller 内部 record）
String executionId;
String agentId;
String status;

// AgentExecutionStatusResponse（API DTO，Controller 内部 record）
String executionId;
String agentId;
String status;
int currentIteration;
int maxIterations;
String finalOutput;
String errorMessage;
List<String> thoughtHistory;
List<Map<String, Object>> toolCalls;  // 简化为 Map 视图
long elapsedMs;
```

### 配置结构

```yaml
agent:
  async:
    max-concurrent: 5
    thread-pool-size: 5
    execution-timeout: 120
    task-retain-minutes: 30
    sse-timeout: 600000
```

---

## 5. 重要约束与边界

- 并发控制：`Semaphore(maxConcurrent)` 非阻塞 `tryAcquire()`，失败立即返回，不排队等待
- 超时控制：`Future.get(timeoutSeconds, SECONDS)`，超时后 `future.cancel(true)` 中断执行线程
- Semaphore 释放保证：在 `finally` 块中释放，无论成功/失败/超时
- 内存存储限制：`AgentTaskStore` 使用 `ConcurrentHashMap`，服务重启后丢失，一期可接受
- 任务过期清理：`@Scheduled(fixedRate = 60000)` 每分钟扫描一次，清除超过 `taskRetainMinutes` 的已终态任务
- SSE 不阻塞执行：SSE 客户端断开后，`emitter.onCompletion()` 仅移除 emitter，不中断后台执行
- 原同步接口保留：`AgentExecutionUseCase.execute()` 保持不变，供 DevPlan 等内部编排继续使用
- 不处理的场景：多实例共享任务状态（需 Redis，二期）、任务优先级调度、执行结果持久化到数据库

---

## 6. 下游依赖调用

```
// LLM 调用（已有，不变）
com.exceptioncoder.llm.infrastructure.provider.LLMProviderRouter#route(String model): LLMProvider
com.exceptioncoder.llm.domain.service.LLMProvider#getChatModel(): ChatModel
org.springframework.ai.chat.model.ChatModel#call(Prompt): ChatResponse

// 工具执行（已有，不变）
com.exceptioncoder.llm.infrastructure.agent.tool.ToolExecutor#execute(String toolId, Map input): String

// Agent 定义查询（已有，不变）
com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository#findById(String id): Optional<AgentDefinition>
```

---

## 7. 异常处理要点

- 并发超限 → `Semaphore.tryAcquire()` 返回 false → Controller 返回 `ResponseEntity.status(429).body(errorMsg)`
- Agent 不存在 → 抛出 `IllegalArgumentException` → Controller 返回 404
- 线程池拒绝 → 捕获 `RejectedExecutionException` → 释放 Semaphore → Controller 返回 503
- 整体超时 → `Future.get` 抛出 `TimeoutException` → `future.cancel(true)` → `AgentTaskStore.update(TIMED_OUT)` → `listener.onError()`
- 单次 LLM 超时 → 捕获 `ResourceAccessException` → `retryCount++`，不超过 2 次则重试当前迭代
- 连续迭代失败 → `consecutiveFailures >= 3` → break 循环 → 状态 FAILED
- SSE 发送失败 → 捕获 `IOException` → 从 `emitterMap` 移除 → 日志 DEBUG → 不影响执行
