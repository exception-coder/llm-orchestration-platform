package com.exceptioncoder.llm.domain.model;

import java.util.List;

/**
 * Agent 执行结果
 */
public record AgentExecutionResult(
        String executionId,
        String agentId,
        String finalOutput,
        List<ToolCall> toolCalls,
        List<String> thoughtHistory,
        int iterations,
        long elapsedMs,
        Status status,
        String errorMessage
) {
    public enum Status {
        SUCCESS,
        FAILED,
        TIMEOUT,
        MAX_ITERATIONS_REACHED
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String executionId;
        private String agentId;
        private String finalOutput;
        private List<ToolCall> toolCalls = List.of();
        private List<String> thoughtHistory = List.of();
        private int iterations;
        private long elapsedMs;
        private Status status = Status.SUCCESS;
        private String errorMessage;

        public Builder executionId(String executionId) { this.executionId = executionId; return this; }
        public Builder agentId(String agentId) { this.agentId = agentId; return this; }
        public Builder finalOutput(String finalOutput) { this.finalOutput = finalOutput; return this; }
        public Builder toolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; return this; }
        public Builder thoughtHistory(List<String> thoughtHistory) { this.thoughtHistory = thoughtHistory; return this; }
        public Builder iterations(int iterations) { this.iterations = iterations; return this; }
        public Builder elapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; return this; }
        public Builder status(Status status) { this.status = status; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }

        public AgentExecutionResult build() {
            return new AgentExecutionResult(executionId, agentId, finalOutput, toolCalls,
                    thoughtHistory, iterations, elapsedMs, status, errorMessage);
        }
    }
}
