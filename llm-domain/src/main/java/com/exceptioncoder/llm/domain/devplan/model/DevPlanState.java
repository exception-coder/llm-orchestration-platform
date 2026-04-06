package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;
import java.util.Map;

/**
 * 开发方案 OverAllState — 跨 Node 共享的全局状态
 * 每个 Node 读取上游写入的数据，追加自身输出
 */
public record DevPlanState(
        String taskId,
        String projectPath,
        String requirement,
        ProjectStructure structure,
        ArchTopology topology,
        RequirementIntent intent,
        ImpactAnalysis impact,
        DevPlanDocument document,
        ValidationResult validation,
        List<String> reviewIssues,
        int correctionCount,
        String status,
        Map<String, Long> nodeTimings,
        Map<String, Integer> agentTokenUsage
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
