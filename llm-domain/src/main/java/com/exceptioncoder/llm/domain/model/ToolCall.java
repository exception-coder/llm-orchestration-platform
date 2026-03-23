package com.exceptioncoder.llm.domain.model;

/**
 * 单次工具调用记录
 */
public record ToolCall(
        String toolName,
        String toolId,
        String inputJson,
        String output,
        long durationMs,
        boolean success,
        String errorMessage
) {
    public static ToolCall success(String toolName, String toolId, String inputJson, String output, long durationMs) {
        return new ToolCall(toolName, toolId, inputJson, output, durationMs, true, null);
    }

    public static ToolCall failure(String toolName, String toolId, String inputJson, String errorMessage, long durationMs) {
        return new ToolCall(toolName, toolId, inputJson, null, durationMs, false, errorMessage);
    }
}
