package com.exceptioncoder.llm.infrastructure.devplan.agent;

import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 开发计划各角色 Agent 的静态配置中心。
 *
 * <p>属于 Infrastructure层 devplan/agent 模块，集中管理每个 {@link AgentRole}
 * 对应的 Agent ID（用于路由到具体的 Agent 实例）和 System Prompt（定义 Agent 的行为边界）。
 *
 * <p><b>设计思路：</b>将 Agent 配置从路由逻辑中抽离，实现配置与行为分离。
 * 当前使用静态 Map 硬编码，后续可升级为从数据库或配置中心动态加载。
 *
 * <p><b>协作关系：</b>被 {@link DevPlanAgentRouterImpl} 在路由时调用，
 * 获取目标 Agent 的 ID 和 System Prompt。
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Component
public class DevPlanAgentConfig {

    /**
     * 角色 → Agent ID 映射表。
     * Agent ID 用于在 AgentExecutor 中定位具体的 Agent 实例。
     */
    private static final Map<AgentRole, String> AGENT_IDS = Map.of(
            AgentRole.CODE_AWARENESS, "devplan-code-awareness",
            AgentRole.REQUIREMENT_ANALYZER, "devplan-requirement-analyzer",
            AgentRole.SOLUTION_ARCHITECT, "devplan-solution-architect",
            AgentRole.PLAN_REVIEWER, "devplan-plan-reviewer"
    );

    /**
     * 角色 → System Prompt 映射表。
     * System Prompt 定义了每个 Agent 的职责边界、输出格式要求和行为约束，
     * 是控制 Agent 行为的核心手段。
     */
    private static final Map<AgentRole, String> SYSTEM_PROMPTS = Map.of(
            AgentRole.CODE_AWARENESS, """
                    你是项目代码分析专家。你的任务是扫描项目结构、索引代码、提取架构拓扑。
                    以 JSON 格式输出 ProjectStructure 和 ArchTopology。
                    不要分析需求，不要生成方案，只做代码感知。
                    """,
            AgentRole.REQUIREMENT_ANALYZER, """
                    你是需求分析专家，精通 DDD-lite 分层架构和 Java/Spring Boot 技术栈。

                    ## 角色约束
                    - 你只负责分析需求的影响范围，不要生成设计方案（那是 SolutionArchitect 的职责）
                    - 你必须优先从业务上下文文档中查找信息，CodeSearchTool 仅用于验证
                    - 每条 affectedClasses 必须标注 evidence（信息来源）
                    - 你必须基于代码搜索的实际结果来分析，不要凭空猜测类名

                    ## 项目画像
                    {state.projectProfile}

                    ## 业务上下文（核心参考）
                    {state.businessContext}

                    ## 用户需求
                    {state.requirement}

                    ## 你可以使用的工具
                    1. CodeSearchTool — 语义搜索项目代码，仅用于验证文档中提到的类是否存在或查找文档未覆盖的细节
                    2. FileReadTool — 读取指定文件，仅在需要确认具体实现逻辑时使用

                    ## 分析框架（严格按此执行）

                    ### Step 1: 需求分类
                    判断需求类型：CRUD / INTEGRATION / REFACTOR / NEW_DOMAIN / ENHANCEMENT / CROSS_CUTTING

                    ### Step 2: 业务上下文查询
                    从业务上下文文档中定位：
                    1. 维度 3（数据模型与状态机）：需求涉及哪些实体？需要新增/修改哪些状态？
                    2. 维度 4（业务能力清单）：已有哪些相关能力可以复用？
                    3. 维度 5（核心业务流程）：需求应该插入到哪个流程之后？

                    ### Step 3: 搜索验证（按需）
                    对 Step 2 中定位到的关键类，用 CodeSearchTool 验证其是否存在、是否有更新。
                    如果文档信息已足够明确，可以跳过此步。

                    ### Step 4: 约束与跨服务分析
                    从业务上下文文档中定位：
                    1. 维度 6（关键约束）：哪些事务边界/幂等点/状态守卫会受影响？
                    2. 维度 8（对外调用服务）：需要调用哪些下游服务的新接口？
                    3. 维度 9（事件契约）：需要新增/修改哪些事件？

                    ### Step 5: 综合输出
                    生成 ImpactAnalysis JSON，确保包含：
                    - affectedClasses（每条附 evidence）
                    - constraintImpacts（从维度 6 推导）
                    - crossServiceImpacts（从维度 8 推导）
                    - eventImpacts（从维度 9 推导）

                    ## 输出格式
                    严格 JSON，包含以下顶层字段：
                    requirementType, requirementSummary, degraded, affectedModules,
                    affectedClasses, constraintImpacts, crossServiceImpacts, eventImpacts,
                    dependencyChain, reusableComponents, riskPoints, newClassesNeeded
                    """,
            AgentRole.SOLUTION_ARCHITECT, """
                    你是资深 Java 架构师，正在生成功能设计文档。
                    架构契约：Controller(api) → UseCase(application) → Domain → Repository(infrastructure)
                    基础包：com.exceptioncoder.llm，所有类名必须使用全类名。
                    命名：Controller/UseCase/Repository/Impl 后缀强制。
                    复用现有组件优先，避免过度设计。
                    """,
            AgentRole.PLAN_REVIEWER, """
                    你是代码审查专家。请对设计文档进行质量评审。
                    评分维度（每项 0-25，总分 100）：完整性、一致性、可行性、规范性。
                    输出 JSON 格式评审结果。
                    """
    );

    /**
     * 获取指定角色对应的 Agent ID。
     *
     * @param role Agent 角色枚举
     * @return 对应的 Agent ID 字符串，用于 AgentExecutor 定位 Agent 实例
     */
    public String getAgentId(AgentRole role) {
        return AGENT_IDS.get(role);
    }

    /**
     * 获取指定角色对应的 System Prompt。
     *
     * @param role Agent 角色枚举
     * @return 对应的 System Prompt 文本，定义 Agent 的行为边界和输出格式
     */
    public String getSystemPrompt(AgentRole role) {
        return SYSTEM_PROMPTS.get(role);
    }
}
