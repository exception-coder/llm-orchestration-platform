package com.exceptioncoder.llm.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Graph 执行结果
 */
public record GraphExecutionResult(
        String executionId,
        String graphId,
        String finalOutput,
        Map<String, Object> context,
        List<NodeExecutionResult> nodeResults,
        long elapsedMs,
        Status status,
        String errorMessage
) {
    public enum Status {
        SUCCESS,
        FAILED,
        PARTIAL
    }

    public record NodeExecutionResult(
            String nodeId,
            String nodeName,
            String output,
            long elapsedMs,
            boolean success,
            String errorMessage
    ) {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String executionId;
        private String graphId;
        private String finalOutput;
        private Map<String, Object> context = Map.of();
        private List<NodeExecutionResult> nodeResults = List.of();
        private long elapsedMs;
        private Status status = Status.SUCCESS;
        private String errorMessage;

        public Builder executionId(String executionId) { this.executionId = executionId; return this; }
        public Builder graphId(String graphId) { this.graphId = graphId; return this; }
        public Builder finalOutput(String finalOutput) { this.finalOutput = finalOutput; return this; }
        public Builder context(Map<String, Object> context) { this.context = context; return this; }
        public Builder nodeResults(List<NodeExecutionResult> nodeResults) { this.nodeResults = nodeResults; return this; }
        public Builder elapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; return this; }
        public Builder status(Status status) { this.status = status; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }

        public GraphExecutionResult build() {
            return new GraphExecutionResult(executionId, graphId, finalOutput, context,
                    nodeResults, elapsedMs, status, errorMessage);
        }
    }
}
