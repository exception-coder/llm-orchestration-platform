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
- 文件：`通用智能体架构-20260404-v1.md`
- 摘要：定义通用 Agent 7 核心环节（Input/Memory/Planning/Reasoning/Tool Selection/Tool Execution/Observation/Output），新增 AgentLoop 核心接口及 5 个策略接口，提供 ReActLoop 实现
- 大纲：背景与目标 / 功能范围 / 7 环节业务流程 / 接口设计 / 类设计 / 核心业务规则 / 异常处理 / 测试要点 / 上线方案

### 多平台模型路由层（子模块）
- 文件：`通用智能体架构/多平台模型路由层/多平台模型路由层-20260331-v1.md`
- 摘要：统一封装 OpenAI/Ollama/阿里云 DashScope 多平台 LLM 调用，支持动态模型切换与成本优化
- 大纲：背景与目标 / 功能范围 / 业务流程 / 接口设计 / 类设计 / 核心业务规则
- 归属原因：模型路由层是通用智能体架构 LLMProvider 环节的基础设施实现

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

### Tool 层与 Agent 初始化器实现（子模块）
- 文件：`代码感知智能开发方案智能体/Tool层与Agent初始化器实现/Tool层与Agent初始化器实现-20260407-v2.md`（最新）
- 摘要：8 个 @Tool 实现（4 元数据提取器 + 1 向量索引器 + 2 检索/读取器 + 1 模板器）+ DevPlanToolRegistry 角色路由 + DevPlanAgentInitializer 启动注册 + Trace 全链路追踪
- 大纲：背景与 v1 问题 / Tool 职责分离原则 / 8 Tool 详细设计 / DevPlanToolRegistry / DevPlanAgentInitializer / Trace / 类清单 / 异常处理
- 归属原因：v2 总体架构中 Tool/Trace/Initializer 的基础设施实现设计
- 历史版本：`Tool层与Agent初始化器实现-20260407-v1.md`（Tool 混淆了提取与理解职责）

### 代码感知智能体实现（子模块）

- 文件：`代码感知智能开发方案智能体/代码感知智能体实现/代码感知智能体实现-20260408-v1.md`
- 摘要：CODE_AWARENESS Agent 完整设计 — 5 Tool ReAct 编排 + ProjectProfile 7 维度生成 + ArchTopology 输出，模型选择 qwen-max
- 大纲：技术选型决策 / 角色定位 / ReAct 执行流程 / 工具集 / System Prompt / 记忆体系 / State 交互 / Agent 注册 / 类清单 / 业务规则 / 异常处理
- 归属原因：StateGraph 第一站 ScanNode 的 Agent 实现，输出供后续所有 Agent 消费

### 需求分析智能体实现（子模块）

- 文件：`代码感知智能开发方案智能体/需求分析智能体实现/需求分析智能体实现-20260408-v1.md`
- 摘要：REQUIREMENT_ANALYZER Agent 完整设计 — CodeSearchTool + FileReadTool ReAct 编排 + ImpactAnalysis JSON 输出，模型选择 qwen-plus
- 大纲：技术选型决策 / 角色定位 / ReAct 执行流程 / 工具集 / System Prompt / 记忆体系 / State 交互 / Agent 注册 / 类清单 / 业务规则 / 异常处理
- 归属原因：StateGraph 第二站 AnalyzeNode 的 Agent 实现

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

### 平台管理接口补全（子模块）

- 文件：`通用智能体架构/平台管理接口补全/平台管理接口补全-20260409-v1.md`
- 摘要：补全 5 大管理对象的关联查询接口 — Graph 下 Agent 列表、调用链视图、Agent 下 Tool 详情、模型平台清单
- 大纲：背景与目标 / 功能范围 / 4 个新增 GET 接口设计 / 类设计（3 Controller + 3 UseCase 改造）/ 核心业务规则
- 归属原因：管理接口操作的对象（Graph/Agent/Tool/ModelConfig）全部定义在通用智能体架构中
