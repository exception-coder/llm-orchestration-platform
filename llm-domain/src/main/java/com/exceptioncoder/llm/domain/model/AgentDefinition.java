package com.exceptioncoder.llm.domain.model;

import java.util.List;

/**
 * Agent 定义
 */
public record AgentDefinition(
        String id,
        String name,
        String description,
        String systemPrompt,
        List<String> toolIds,
        String llmProvider,
        String llmModel,
        Integer maxIterations,
        Integer timeoutSeconds,
        boolean enabled
) {
    public AgentDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Agent id 不能为空");
        }
        if (maxIterations == null) maxIterations = 10;
        if (timeoutSeconds == null) timeoutSeconds = 120;
        if (enabled == false) enabled = true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private String systemPrompt;
        private List<String> toolIds;
        private String llmProvider;
        private String llmModel;
        private Integer maxIterations = 10;
        private Integer timeoutSeconds = 120;
        private boolean enabled = true;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder systemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; return this; }
        public Builder toolIds(List<String> toolIds) { this.toolIds = toolIds; return this; }
        public Builder llmProvider(String llmProvider) { this.llmProvider = llmProvider; return this; }
        public Builder llmModel(String llmModel) { this.llmModel = llmModel; return this; }
        public Builder maxIterations(Integer maxIterations) { this.maxIterations = maxIterations; return this; }
        public Builder timeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }

        public AgentDefinition build() {
            return new AgentDefinition(id, name, description, systemPrompt, toolIds,
                    llmProvider, llmModel, maxIterations, timeoutSeconds, enabled);
        }
    }
}
