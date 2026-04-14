# 阅读引导

> 本文档为 docs/ 下所有文档的建议阅读顺序，帮助读者由浅入深理解项目全貌。
>
> **维护规则：** 文档新增、移动或删除时须同步更新本文件。

---

## 第一阶段：项目概览

从整体架构和目录结构入手，建立全局认知。

| 序号 | 文档 | 说明 |
|:---:|------|------|
| 1 | [docs/INDEX.md](INDEX.md) | 文档总索引，了解文档分类方式 |
| 2 | [docs/README.md](README.md) | 各目录说明与维护规范 |
| 3 | [design/architecture/architecture-20260322.md](design/architecture/architecture-20260322.md) | 系统架构设计（含架构图） |

---

## 第二阶段：技术选型与基础设施

理解项目为什么选择当前技术栈、核心路由与限速降级机制。

| 序号 | 文档 | 说明 |
|:---:|------|------|
| 4 | [design/通用智能体架构/技术选型/Java大模型应用组件全景图-v4.md](design/通用智能体架构/技术选型/Java大模型应用组件全景图-v4.md) | Java 大模型应用 11 层全景图（最新版） |
| 5 | [design/通用智能体架构/技术选型/组件复用与框架选型分析.md](design/通用智能体架构/技术选型/组件复用与框架选型分析.md) | Spring AI vs LangChain4j 及周边组件选型 |
| 6 | [design/通用智能体架构/多平台模型路由层/多平台模型路由层-20260331-v1.md](design/通用智能体架构/多平台模型路由层/多平台模型路由层-20260331-v1.md) | 多平台 LLM 统一路由层设计 |
| 7 | [design/通用智能体架构/多平台模型路由层/ChatModel统一获取与限速降级-20260409-v1.md](design/通用智能体架构/多平台模型路由层/ChatModel统一获取与限速降级-20260409-v1.md) | ChatModel 获取、限速与降级机制 |

---

## 第三阶段：通用智能体架构

掌握平台级 Agent 核心抽象，这是所有业务智能体的基础。

| 序号 | 文档 | 说明 |
|:---:|------|------|
| 8 | [design/通用智能体架构/通用智能体架构-20260404-v1.md](design/通用智能体架构/通用智能体架构-20260404-v1.md) | Agent 7 核心环节 + ReActLoop 实现 |
| 9 | [design/通用智能体架构/Agent全链路Trace设计/Agent全链路Trace设计-20260412-v1.md](design/通用智能体架构/Agent全链路Trace设计/Agent全链路Trace设计-20260412-v1.md) | 全链路追踪标准 |
| 10 | [design/通用智能体架构/Agent异步执行机制/Agent异步执行机制-20260413-v2.md](design/通用智能体架构/Agent异步执行机制/Agent异步执行机制-20260413-v2.md) | 异步提交 + SSE Flux 推送 |
| 11 | [design/通用智能体架构/Graph可视化编排/Graph可视化编排-20260412-v1.md](design/通用智能体架构/Graph可视化编排/Graph可视化编排-20260412-v1.md) | Vue Flow DAG 可视化编排 |
| 12 | [design/通用智能体架构/平台管理接口补全/平台管理接口补全-20260409-v1.md](design/通用智能体架构/平台管理接口补全/平台管理接口补全-20260409-v1.md) | Graph/Agent/Tool 管理接口 |

---

## 第四阶段：业务智能体设计

在通用架构基础上，了解各业务智能体的具体设计。

### 4a. 代码感知智能开发方案智能体（DevPlan）

按流程阶段顺序阅读：总体设计 → 四个 Agent 实现 → Tool 层。

| 序号 | 文档 | 说明 |
|:---:|------|------|
| 13 | [design/代码感知智能开发方案智能体/整体方案设计-20260406-v2.md](design/代码感知智能开发方案智能体/整体方案设计-20260406-v2.md) | 总体设计：4 角色 + StateGraph 编排 |
| 14 | [design/代码感知智能开发方案智能体/代码感知智能体实现/代码感知智能体实现-20260412-v3.md](design/代码感知智能开发方案智能体/代码感知智能体实现/代码感知智能体实现-20260412-v3.md) | 第一站 ScanNode：代码感知 |
| 15 | [design/代码感知智能开发方案智能体/需求分析智能体实现/需求分析智能体实现-20260412-v2.md](design/代码感知智能开发方案智能体/需求分析智能体实现/需求分析智能体实现-20260412-v2.md) | 第二站 AnalyzeNode：需求分析 |
| 16 | [design/代码感知智能开发方案智能体/方案生成智能体实现/方案生成智能体实现-20260408-v1.md](design/代码感知智能开发方案智能体/方案生成智能体实现/方案生成智能体实现-20260408-v1.md) | 第三站 DesignNode：方案生成 |
| 17 | [design/代码感知智能开发方案智能体/方案审查智能体实现/方案审查智能体实现-20260408-v1.md](design/代码感知智能开发方案智能体/方案审查智能体实现/方案审查智能体实现-20260408-v1.md) | 第四站 ReviewNode：方案审查 |
| 18 | [design/代码感知智能开发方案智能体/Tool层与Agent初始化器实现/Tool层实现设计-20260412-v1.md](design/代码感知智能开发方案智能体/Tool层与Agent初始化器实现/Tool层实现设计-20260412-v1.md) | 8 个 Tool 实现设计 |
| 19 | [design/代码感知智能开发方案智能体/Tool层与Agent初始化器实现/Agent初始化器实现设计-20260412-v1.md](design/代码感知智能开发方案智能体/Tool层与Agent初始化器实现/Agent初始化器实现设计-20260412-v1.md) | Agent 初始化器设计 |

### 4b. 其他业务智能体

| 序号 | 文档 | 说明 |
|:---:|------|------|
| 20 | [design/智能助手/智能助手-20260329-v1.md](design/智能助手/智能助手-20260329-v1.md) | 个人秘书智能体：日程/待办/笔记 |
| 21 | [design/文档查看器/文档查看器-20260330-v2.md](design/文档查看器/文档查看器-20260330-v2.md) | 文档解析与结构化智能体 |
| 22 | [design/大文本块提取工具/大文本块提取工具-20260402-v1.md](design/大文本块提取工具/大文本块提取工具-20260402-v1.md) | 大文本块提取工具设计 |

---

## 第五阶段：开发者指南与使用手册

准备动手开发或使用平台功能时参考。

### 开发者指南

| 序号 | 文档 | 说明 |
|:---:|------|------|
| 23 | [developer-guide/agent-development-guide.md](developer-guide/agent-development-guide.md) | 新智能体开发完整流程 |
| 24 | [developer-guide/ddd-practice-sse-layer-decision.md](developer-guide/ddd-practice-sse-layer-decision.md) | DDD 分层实践案例：SSE 归属决策 |
| 25 | [developer-guide/best-practice-stream-collect-for-llm-timeout.md](developer-guide/best-practice-stream-collect-for-llm-timeout.md) | 最佳实践：Stream-Collect 解决 LLM 超时 |

### 使用指南

| 序号 | 文档 | 说明 |
|:---:|------|------|
| 26 | [guides/configuration-guide.md](guides/configuration-guide.md) | 配置文件说明 |
| 27 | [guides/qdrant-integration-guide.md](guides/qdrant-integration-guide.md) | Qdrant 向量存储集成 |
| 28 | [guides/prompt-template-management.md](guides/prompt-template-management.md) | Prompt 模板管理 |
| 29 | [guides/prompt-test-guide.md](guides/prompt-test-guide.md) | Prompt 测试功能 |
| 30 | [guides/content-optimization-guide.md](guides/content-optimization-guide.md) | 内容优化功能 |

---

## 第六阶段：运维与参考

| 序号 | 文档 | 说明 |
|:---:|------|------|
| 31 | [sql/init-database.sql](sql/init-database.sql) | 数据库初始化脚本 |
| 32 | [sql/init-model-config.sql](sql/init-model-config.sql) | 模型配置初始化数据 |
| 33 | [dev/dev-log.md](dev/dev-log.md) | 开发日志索引 |
| 34 | [dev/pitfall-spring-ai-version.md](dev/pitfall-spring-ai-version.md) | 踩坑记录 |
| 35 | [coding-violations.md](coding-violations.md) | 编码违规记录 |

---

## 阅读建议

- **新成员入门**：按第一至第三阶段顺序完整阅读，建立架构认知后再按需进入第四阶段
- **新智能体开发**：第三阶段 + 第 23 篇开发指南 + 第四阶段选一个智能体作为参考
- **了解特定功能**：通过第一阶段索引定位目标文档直接阅读
- **排查问题**：优先查看第 34 篇踩坑记录和第 25 篇超时最佳实践
