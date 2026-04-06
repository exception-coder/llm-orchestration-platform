package com.exceptioncoder.llm.domain.devplan.model;

import java.time.LocalDateTime;

/**
 * 开发方案任务实体
 */
public record DevPlanTask(
        String taskId,
        String projectPath,
        String requirement,
        String status,
        String currentNode,
        int priority,
        int timeoutSeconds,
        String traceId,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
    public enum Status {
        CREATED, QUEUED, RUNNING, COMPLETED, FAILED, TIMED_OUT
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String taskId;
        private String projectPath;
        private String requirement;
        private String status = Status.CREATED.name();
        private String currentNode;
        private int priority;
        private int timeoutSeconds = 300;
        private String traceId;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder taskId(String taskId) { this.taskId = taskId; return this; }
        public Builder projectPath(String projectPath) { this.projectPath = projectPath; return this; }
        public Builder requirement(String requirement) { this.requirement = requirement; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder currentNode(String currentNode) { this.currentNode = currentNode; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder timeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; return this; }
        public Builder traceId(String traceId) { this.traceId = traceId; return this; }
        public Builder startedAt(LocalDateTime startedAt) { this.startedAt = startedAt; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public DevPlanTask build() {
            return new DevPlanTask(taskId, projectPath, requirement, status, currentNode,
                    priority, timeoutSeconds, traceId, startedAt, completedAt, createdAt);
        }
    }
}
