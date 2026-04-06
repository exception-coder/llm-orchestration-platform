package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;
import java.util.Map;

/**
 * 开发方案全局状态（OverAllState） -- 跨 Node 共享的不可变状态快照。
 *
 * <p>属于 Domain 层 devplan 模块。在 LangGraph 风格的有向图中，每个 Node
 * 读取上游写入的数据并追加自身输出，形成逐步富化的状态链。Record 天然不可变，
 * 配合 {@link Builder} 实现"读旧 → 构建新"的函数式状态流转。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record DevPlanState(
        String taskId,                          // 任务唯一标识
        String projectPath,                     // 目标项目的本地路径
        String requirement,                     // 用户输入的原始需求文本
        ProjectStructure structure,             // 代码感知阶段输出的项目结构
        ArchTopology topology,                  // 代码感知阶段输出的架构拓扑
        RequirementIntent intent,               // 需求分析阶段输出的意图识别结果
        ImpactAnalysis impact,                  // 需求分析阶段输出的影响范围分析
        DevPlanDocument document,               // 方案设计阶段输出的设计文档
        ValidationResult validation,            // 方案审查阶段输出的验证结果
        List<String> reviewIssues,              // 审查发现的问题列表（用于修正循环）
        int correctionCount,                    // 当前已执行的修正次数
        String status,                          // 任务当前状态（INIT / RUNNING / COMPLETED / FAILED）
        Map<String, Long> nodeTimings,          // 各 Node 执行耗时（毫秒），key 为节点名称
        Map<String, Integer> agentTokenUsage    // 各 Agent Token 消耗量，key 为角色名称
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String taskId;
        private String projectPath;
        private String requirement;
        private ProjectStructure structure;
        private ArchTopology topology;
        private RequirementIntent intent;
        private ImpactAnalysis impact;
        private DevPlanDocument document;
        private ValidationResult validation;
        private List<String> reviewIssues = List.of();
        private int correctionCount;
        private String status = "INIT";
        private Map<String, Long> nodeTimings = new java.util.HashMap<>();
        private Map<String, Integer> agentTokenUsage = new java.util.HashMap<>();

        public Builder taskId(String taskId) { this.taskId = taskId; return this; }
        public Builder projectPath(String projectPath) { this.projectPath = projectPath; return this; }
        public Builder requirement(String requirement) { this.requirement = requirement; return this; }
        public Builder structure(ProjectStructure structure) { this.structure = structure; return this; }
        public Builder topology(ArchTopology topology) { this.topology = topology; return this; }
        public Builder intent(RequirementIntent intent) { this.intent = intent; return this; }
        public Builder impact(ImpactAnalysis impact) { this.impact = impact; return this; }
        public Builder document(DevPlanDocument document) { this.document = document; return this; }
        public Builder validation(ValidationResult validation) { this.validation = validation; return this; }
        public Builder reviewIssues(List<String> reviewIssues) { this.reviewIssues = reviewIssues; return this; }
        public Builder correctionCount(int correctionCount) { this.correctionCount = correctionCount; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder nodeTimings(Map<String, Long> nodeTimings) { this.nodeTimings = nodeTimings; return this; }
        public Builder agentTokenUsage(Map<String, Integer> agentTokenUsage) { this.agentTokenUsage = agentTokenUsage; return this; }

        public DevPlanState build() {
            return new DevPlanState(taskId, projectPath, requirement, structure, topology,
                    intent, impact, document, validation, reviewIssues, correctionCount,
                    status, nodeTimings, agentTokenUsage);
        }
    }
}
