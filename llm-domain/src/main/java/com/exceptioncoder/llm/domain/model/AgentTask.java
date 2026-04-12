package com.exceptioncoder.llm.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent 执行任务生命周期模型。
 *
 * <p>记录一次 Agent 异步执行的完整状态，包括迭代进度、思考历史、
 * 工具调用记录和耗时统计。支持 Builder 模式进行函数式状态转换。</p>
 */
public record AgentTask(
        String executionId,
        String agentId,
        Status status,
        int currentIteration,
        int maxIterations,
        String finalOutput,
        String errorMessage,
        List<String> thoughtHistory,
        List<ToolCall> toolCalls,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
    public enum Status {
        SUBMITTED, RUNNING, COMPLETED, FAILED, TIMED_OUT
    }

    /**
     * 计算已耗时（毫秒）。
     */
    public long elapsedMs() {
        if (startedAt == null) return 0;
        LocalDateTime end = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, end).toMillis();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .executionId(executionId)
                .agentId(agentId)
                .status(status)
                .currentIteration(currentIteration)
                .maxIterations(maxIterations)
                .finalOutput(finalOutput)
                .errorMessage(errorMessage)
                .thoughtHistory(new ArrayList<>(thoughtHistory))
                .toolCalls(new ArrayList<>(toolCalls))
                .startedAt(startedAt)
                .completedAt(completedAt)
                .createdAt(createdAt);
    }

    public static class Builder {
        private String executionId;
        private String agentId;
        private Status status = Status.SUBMITTED;
        private int currentIteration;
        private int maxIterations = 10;
        private String finalOutput;
        private String errorMessage;
        private List<String> thoughtHistory = new ArrayList<>();
        private List<ToolCall> toolCalls = new ArrayList<>();
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder executionId(String executionId) { this.executionId = executionId; return this; }
        public Builder agentId(String agentId) { this.agentId = agentId; return this; }
        public Builder status(Status status) { this.status = status; return this; }
        public Builder currentIteration(int currentIteration) { this.currentIteration = currentIteration; return this; }
        public Builder maxIterations(int maxIterations) { this.maxIterations = maxIterations; return this; }
        public Builder finalOutput(String finalOutput) { this.finalOutput = finalOutput; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder thoughtHistory(List<String> thoughtHistory) { this.thoughtHistory = thoughtHistory; return this; }
        public Builder toolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; return this; }
        public Builder startedAt(LocalDateTime startedAt) { this.startedAt = startedAt; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public AgentTask build() {
            return new AgentTask(executionId, agentId, status, currentIteration, maxIterations,
                    finalOutput, errorMessage, thoughtHistory, toolCalls,
                    startedAt, completedAt, createdAt);
        }
    }
}
