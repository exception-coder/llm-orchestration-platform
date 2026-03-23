package com.exceptioncoder.llm.domain.model;


/**
 * 工具定义
 */
public record ToolDefinition(
        String id,
        String name,
        String description,
        String inputSchema,
        ToolType type,
        boolean isAsync
) {
    public ToolDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Tool id 不能为空");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool name 不能为空");
        }
        if (type == null) type = ToolType.FUNCTION;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private String inputSchema;
        private ToolType type = ToolType.FUNCTION;
        private boolean isAsync = false;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder inputSchema(String inputSchema) { this.inputSchema = inputSchema; return this; }
        public Builder type(ToolType type) { this.type = type; return this; }
        public Builder isAsync(boolean isAsync) { this.isAsync = isAsync; return this; }

        public ToolDefinition build() {
            return new ToolDefinition(id, name, description, inputSchema, type, isAsync);
        }
    }
}
