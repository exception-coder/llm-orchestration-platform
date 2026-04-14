# design/ 文档索引

## 智能助手
- 文件：`智能助手-20260329-v1.md`
- 摘要：个人秘书智能体设计，包含日程管理、待办管理、笔记搜索三大工具能力
- 大纲：背景与目标 / 功能范围 / Agent 执行流程 / 工具定义 / 接口设计 / 类设计 / 数据库设计 / 核心业务规则

## 文档查看器
- 文件：`文档查看器-20260330-v2.md`
- 摘要：文档解析与结构化智能体，支持 Markdown 文档解析、目录生成、摘要提取
- 大纲：背景与目标 / 功能范围 / 业务流程 / 接口设计 / 类设计 / 数据库设计 / 核心业务规则

## 通用智能体架构
- 文件：`通用智能体架构-20260414-v2.md`（最新）
- 摘要：对齐代码实际实现的架构基线文档 — AgentExecutor + ReAct 循环、GraphExecutionEngine DAG 编排、异步执行 + Flux SSE、@Tool 注解驱动注册、@AgentGroup 多智能体编排、三级记忆体系、全链路 Trace、LLMProviderRouter 限速降级
- 大纲：背景与 v1 差异说明 / 功能模块总览图 / Agent ReAct 执行流程 / Graph DAG 编排流程 / 异步执行流程 / 接口设计（6 核心接口）/ 类设计（35+ 类）/ 业务规则（R1-R10）/ Provider 路由降级 / Trace / 记忆体系
- 历史版本：`通用智能体架构-20260404-v1.md`（初始蓝图，AgentLoop + 5 策略接口设计，未按原方案实施）

### 多平台模型路由层（子模块）
- 文件：`通用智能体架构/多平台模型路由层/多平台模型路由层-20260331-v1.md`
- 摘要：统一封装 OpenAI/Ollama/阿里云 DashScope 多平台 LLM 调用，支持动态模型切换与成本优化
- 大纲：背景与目标 / 功能范围 / 业务流程 / 接口设计 / 类设计 / 核心业务规则
- 归属原因：模型路由层是通用智能体架构 LLMProvider 环节的基础设施实现

### ChatModel 统一获取与限速降级（子模块）
- 文件：`通用智能体架构/多平台模型路由层/ChatModel统一获取与限速降级-20260409-v1.md`
- 摘要：消除 AgentExecutor 重复构建 ChatModel 的问题，统一通过 Router 获取；Provider 层增加 Guava RateLimiter 主动限速；Router 层增加 Fallback 降级
- 大纲：背景与目标 / 功能范围 / 业务流程（ChatModel 获取/限速/Fallback）/ 类设计（9 文件变更）/ 核心业务规则 / 异常处理 / 测试要点
- 归属原因：多平台模型路由层 v1 的增强，解决 Agent 场景 429 限流问题

### 技术选型（子模块）

- 文件：`通用智能体架构/技术选型/Java大模型应用组件全景图.md`
- 摘要：Java 大模型应用 11 层全景图（v1 基础版），覆盖接入层到基础设施层全链路候选技术与优缺点
- 大纲：全景图 Mermaid / 图例说明
- 附加版本：`Java大模型应用组件全景图-v2.md`（新增 7 组对比分析表格：Agent框架 / Spring AI vs Alibaba / 模型 / 向量库 / Embedding / 记忆存储 / 可观测性）

- 文件：`通用智能体架构/技术选型/组件复用与框架选型分析.md`
- 摘要：从企业级 Agent 架构视角完整覆盖两大核心框架对比及周边 8 类组件选型决策
- 大纲：全景图速查 / 核心框架对比(Spring AI vs LangChain4j) / 向量数据库 / Embedding模型 / 文档处理 / MCP协议 / 评估与质量 / 安全与合规 / 可观测性 / 全组件决策总表
- 归属原因：技术选型是通用智能体架构落地实施前的框架与组件决策依据

## 代码感知智能开发方案智能体
- 文件：`代码感知智能开发方案智能体/代码感知智能开发方案智能体-20260406-v2.md`（最新）
- 摘要：基于组件全景图 v4 架构（L4 可控多 Agent 系统），引入控制平面 + StateGraph 流程编排 + 4 角色 Agent 协作 + Tool 标准协议 + 三级记忆体系 + 全链路 Trace
- 大纲：背景与目标 / 四角色协作模型 / StateGraph 流程设计 / 接口设计 / 类设计（55 个新建类 + 8 个复用类）/ 数据库设计（4 表）/ 核心业务规则 / v4 全景图映射 / 架构成熟度对标 / Prompt 模板
- 历史版本：`代码感知智能开发方案智能体-20260406-v1.md`（基于 v3 全景图，单 Agent 四阶段串行）

### Tool 层实现设计（子模块）
- 文件：`代码感知智能开发方案智能体/Tool层与Agent初始化器实现/Tool层实现设计-20260412-v1.md`（最新）
- 摘要：8 个 @Tool 实现（4 元数据提取器 + 1 向量索引器 + 2 检索/读取器 + 1 模板器）+ DevPlanToolRegistry 角色路由 + Schema 校验
- 大纲：背景与 v1 问题 / Tool 职责分离原则 / 8 Tool 详细设计 / DevPlanToolRegistry / 类清单 / 技术方案 / 异常处理
- 归属原因：v2 总体架构中 Tool 标准协议层的实现设计

### Agent 初始化器实现设计（子模块）
- 文件：`代码感知智能开发方案智能体/Tool层与Agent初始化器实现/Agent初始化器实现设计-20260412-v1.md`（最新）
- 摘要：DevPlanAgentInitializer 启动时将 4 个角色 Agent 写入 agent_definition 表，幂等设计，启动顺序依赖
- 大纲：背景 / Agent 记录定义 / 启动顺序依赖 / 类清单 / 业务规则 / 异常处理
- 归属原因：v2 总体架构中 Agent 启动注册的实现设计

> **归档：** `Tool层与Agent初始化器实现-20260407-v2.md` 为拆分前的合并文档，不再维护。
> 历史版本：`Tool层与Agent初始化器实现-20260407-v1.md`（Tool 混淆了提取与理解职责）

### 代码感知智能体实现（子模块）

- 文件：`代码感知智能开发方案智能体/代码感知智能体实现/代码感知智能体实现-20260412-v3.md`（最新）
- 摘要：ClaudeCodeProfileGenerator v3 改造 — SDK 桥接模式（主路径）+ CLI 备选、空闲超时替代固定总超时、结构化事件流日志、双模式配置切换
- 大纲：v2 问题分析 / 双模式架构设计 / SDK 桥接脚本协议 / 空闲超时机制 / 配置扩展 / 类设计（1 重构 + 1 修改 + 2 新建脚本）/ 业务规则（R11-R15）/ 异常处理 / 测试要点
- 归属原因：StateGraph 第一站 ScanNode 的 Agent 实现，输出供后续所有 Agent 消费
- 历史版本：`代码感知智能体实现-20260411-v2.md`（SPI 生成器链 + CLI 主实现）、`代码感知智能体实现-20260408-v1.md`（5 Tool ReAct 编排）

### 需求分析智能体实现（子模块）

- 文件：`代码感知智能开发方案智能体/需求分析智能体实现/需求分析智能体实现-20260412-v2.md`（最新）
- 摘要：适配画像 3 文件拆分，执行模式从"搜索猜测"改为"文档查询 + 搜索验证"，ImpactAnalysis 输出增强（constraintImpacts / crossServiceImpacts / eventImpacts），System Prompt 重构
- 大纲：v1 问题分析 / v2 文档查询模式 / State 输入适配 / Prompt 重构 / ImpactAnalysis 扩展 / 降级兼容 / 异常处理 / 测试要点
- 归属原因：StateGraph 第二站 AnalyzeNode 的 Agent 实现
- 历史版本：`需求分析智能体实现-20260408-v1.md`（纯搜索猜测模式）

### 方案生成智能体实现（子模块）

- 文件：`代码感知智能开发方案智能体/方案生成智能体实现/方案生成智能体实现-20260408-v1.md`
- 摘要：SOLUTION_ARCHITECT Agent 完整设计 — CodeSearchTool + TemplateRenderTool 编排 + 设计文档 Markdown 输出 + 修正循环机制
- 大纲：技术选型决策 / 角色定位 / 执行流程 / 工具集 / 模板系统 / System Prompt / 修正循环 / State 交互 / Agent 注册 / 类清单
- 归属原因：StateGraph 第三站 DesignNode 的 Agent 实现

### 方案审查智能体实现（子模块）

- 文件：`代码感知智能开发方案智能体/方案审查智能体实现/方案审查智能体实现-20260408-v1.md`
- 摘要：PLAN_REVIEWER Agent 完整设计 — 无外部 Tool，使用 PlanSensor 传感器链（计算型 + 推理型 + 聚合型）进行方案评审
- 大纲：技术选型决策 / Tool vs Sensor 对比 / 角色定位 / Sensor Chain 设计 / System Prompt / State 交互 / Agent 注册 / 类清单
- 归属原因：StateGraph 第四站 ReviewNode 的 Agent 实现

### Agent 全链路 Trace 设计（子模块）

- 文件：`通用智能体架构/Agent全链路Trace设计/Agent全链路Trace设计-20260412-v1.md`
- 摘要：平台级 Agent 全链路追踪标准 — SpanContext 数据模型 + AgentTraceRecorder 组件（ThreadLocal Span 栈 + SLF4J 日志），一期内存实现，预留 OpenTelemetry 演进路径
- 大纲：背景与目标 / 核心概念（Trace/Span/调用树）/ SpanContext record / AgentTraceRecorder 组件 / 使用模式 / 类清单 / 业务规则 / 演进规划
- 归属原因：从 devplan Trace 设计中提取通用化，所有 Agent Group 共用的可观测性基础设施

### 平台管理接口补全（子模块）

- 文件：`通用智能体架构/平台管理接口补全/平台管理接口补全-20260409-v1.md`
- 摘要：补全 5 大管理对象的关联查询接口 — Graph 下 Agent 列表、调用链视图、Agent 下 Tool 详情、模型平台清单
- 大纲：背景与目标 / 功能范围 / 4 个新增 GET 接口设计 / 类设计（3 Controller + 3 UseCase 改造）/ 核心业务规则
- 归属原因：管理接口操作的对象（Graph/Agent/Tool/ModelConfig）全部定义在通用智能体架构中

### Graph 可视化编排（子模块）

- 文件：`通用智能体架构/Graph可视化编排/Graph可视化编排-20260412-v1.md`
- 摘要：前端使用 Vue Flow + Dagre 以交互式 DAG 流程图展示 Graph 中 Agent 调度关系和 Agent-Tool 绑定关系，支持自定义节点、自动布局、MiniMap、Tool 侧边面板
- 大纲：背景与目标 / 功能范围（总览图+能力分解图）/ 业务流程（初始化+交互+异常）/ 接口设计（复用 4 API）/ 类设计（5 新建+2 改造）/ 核心业务规则 / 下游依赖（5 npm 包）/ 测试要点
- 归属原因：可视化的对象（Graph/Agent/Tool）定义在通用智能体架构中，消费的 API 来自平台管理接口补全

### Agent 异步执行机制（子模块）

- 文件：`通用智能体架构/Agent异步执行机制/Agent异步执行机制-20260413-v2.md`（最新）
- 摘要：Agent 执行从同步阻塞改为异步提交 + 状态轮询 / SSE 推送（Flux 模式）。v2 修正 SSE 分层：AgentSseManager → AgentEventSink + Flux，API 层不再跨层引用 Infrastructure
- 大纲：背景与目标 / 功能范围（5 能力域）/ 业务流程（异步提交+Flux SSE 推送+异常+状态流转）/ 接口设计（3 端点）/ 类设计（13 类：8 新建+5 改造）/ 核心业务规则（R1-R9）/ 并发与超时控制 / Flux 事件流生命周期 / 异常处理 / 测试要点
- 历史版本：`Agent异步执行机制-20260412-v1.md`（方案 A，AgentSseManager 直接管理 SseEmitter）
- 归属原因：改造的 AgentExecutor、AgentExecutionUseCase、AgentController 均定义在通用智能体架构中
