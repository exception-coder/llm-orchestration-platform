package com.exceptioncoder.llm.domain.devplan.model;

import java.time.LocalDateTime;

/**
 * 开发方案任务实体 -- 表示一次开发方案生成的完整生命周期。
 *
 * <p>属于 Domain 层 devplan 模块。每次用户提交需求后会创建一条 DevPlanTask，
 * 任务经过 CREATED → QUEUED → RUNNING → COMPLETED/FAILED/TIMED_OUT 状态流转。
 * 通过 {@link Builder} 创建实例，支持默认值预填充。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record DevPlanTask(
        String taskId,              // 任务唯一标识（UUID）
        String projectPath,         // 目标项目的本地路径
        String requirement,         // 用户输入的原始需求文本
        String status,              // 当前状态，取值参考 {@link Status} 枚举
        String currentNode,         // 当前正在执行的 Graph 节点名称
        int priority,               // 任务优先级，数值越大优先级越高
        int timeoutSeconds,         // 任务超时时间（秒），默认 300
        String traceId,             // 分布式链路追踪 ID
        LocalDateTime startedAt,    // 任务开始执行时间
        LocalDateTime completedAt,  // 任务完成时间
        LocalDateTime createdAt     // 任务创建时间
) {
    /**
     * 任务状态枚举，定义任务生命周期中所有合法状态。
     */
    public enum Status {
        CREATED,    // 已创建，尚未入队
        QUEUED,     // 已入队，等待执行
        RUNNING,    // 正在执行
        COMPLETED,  // 执行成功
        FAILED,     // 执行失败
        TIMED_OUT   // 执行超时
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
