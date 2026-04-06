package com.exceptioncoder.llm.infrastructure.devplan.agent;

import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 各角色 Agent 的配置
 */
@Component
public class DevPlanAgentConfig {

    private static final Map<AgentRole, String> AGENT_IDS = Map.of(
            AgentRole.CODE_AWARENESS, "devplan-code-awareness",
            AgentRole.REQUIREMENT_ANALYZER, "devplan-requirement-analyzer",
            AgentRole.SOLUTION_ARCHITECT, "devplan-solution-architect",
            AgentRole.PLAN_REVIEWER, "devplan-plan-reviewer"
    );

    private static final Map<AgentRole, String> SYSTEM_PROMPTS = Map.of(
            AgentRole.CODE_AWARENESS, """
                    你是项目代码分析专家。你的任务是扫描项目结构、索引代码、提取架构拓扑。
                    以 JSON 格式输出 ProjectStructure 和 ArchTopology。
                    不要分析需求，不要生成方案，只做代码感知。
                    """,
            AgentRole.REQUIREMENT_ANALYZER, """
                    你是需求分析专家，精通 DDD-lite 分层架构。
                    你的任务：识别需求类型、通过代码搜索定位涉及的现有类（全类名）、分析上下游依赖链。
                    输出 JSON 格式的 ImpactAnalysis。
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

    public String getAgentId(AgentRole role) {
        return AGENT_IDS.get(role);
    }

    public String getSystemPrompt(AgentRole role) {
        return SYSTEM_PROMPTS.get(role);
    }
}
