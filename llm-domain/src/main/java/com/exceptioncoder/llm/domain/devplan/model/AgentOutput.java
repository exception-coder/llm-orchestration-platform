package com.exceptioncoder.llm.domain.devplan.model;

import java.util.Map;

/**
 * Agent 执行输出（通用）
 */
public record AgentOutput(
        AgentRole role,
        String rawOutput,
        Map<String, Object> structuredData,
        int tokenUsage,
        long elapsedMs
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AgentRole role;
        private String rawOutput;
        private Map<String, Object> structuredData = Map.of();
        private int tokenUsage;
        private long elapsedMs;

        public Builder role(AgentRole role) { this.role = role; return this; }
        public Builder rawOutput(String rawOutput) { this.rawOutput = rawOutput; return this; }
        public Builder structuredData(Map<String, Object> structuredData) { this.structuredData = structuredData; return this; }
        public Builder tokenUsage(int tokenUsage) { this.tokenUsage = tokenUsage; return this; }
        public Builder elapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; return this; }

        public AgentOutput build() {
            return new AgentOutput(role, rawOutput, structuredData, tokenUsage, elapsedMs);
        }
    }
}
